package com.edu.bootstring.shipment;

import com.edu.bootstring.customer.Customer;
import com.edu.bootstring.customer.CustomerRepository;
import com.edu.bootstring.global.error.exception.InsufficientStockException;
import com.edu.bootstring.inventory.Inventory;
import com.edu.bootstring.inventory.InventoryRepository;
import com.edu.bootstring.order.SalesOrder;
import com.edu.bootstring.order.SalesOrderItem;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.product.Product;
import com.edu.bootstring.product.ProductRepository;
import com.edu.bootstring.shipment.dto.ShipmentDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 출하 확정 동시성 검증.
 *
 * <p>이 테스트는 <b>실제 Oracle</b>(docker compose 의 treader-oracle, localhost:1523)에 붙는다.
 * H2 로는 의미가 없다. 검증 대상이 {@code SELECT ... FOR UPDATE} 의 행 잠금 동작 자체이고,
 * 그건 DB 마다 다르기 때문이다.
 *
 * <p><b>테스트가 증명하려는 것</b><br>
 * "락을 걸었다"는 주장이 아니라 <b>"락을 떼면 이 테스트가 깨진다"</b>는 사실이다.
 * 실제로 떼어 보고 확인한 결과는 두 경우가 서로 달랐다.
 *
 * <p><b>(1) 재고 락을 뗐을 때</b> —
 * {@link com.edu.bootstring.inventory.InventoryRepository#findByProductIdForUpdate} 의 {@code @Lock} 제거.
 * 재고 값이 깨지지는 않는다. {@link com.edu.bootstring.inventory.Inventory} 의 {@code @Version}
 * 낙관적 락이 두 번째 방어선으로 걸려, 10 개 스레드 중 9 개가
 * {@code ObjectOptimisticLockingFailureException} 으로 실패한다.
 * 즉 <b>데이터는 지켜지지만 사용자가 계속 충돌 에러를 본다.</b>
 * 비관적 락의 역할은 정합성 그 자체가 아니라, 경합을 <b>대기로 바꿔 순차 성공시키는 것</b>이다.
 * 이게 두 락의 실질적 차이다.
 *
 * <p><b>(2) 수주 락을 뗐을 때</b> —
 * {@link com.edu.bootstring.order.SalesOrderRepository#findByIdForUpdate} 의 {@code @Lock} 제거.
 * 이쪽은 조용히 깨진다. {@code SALES_ORDER_ITEM} 에는 {@code @Version} 이 없어서 막아 줄 것이 없다.
 * 출하 10 건이 모두 성공하고 재고는 600 이 빠지는데, 장부의 {@code SHIPPED_QTY} 에는 <b>60</b> 만 남는다.
 * 예외가 하나도 뜨지 않으므로 아무도 모른 채 수주와 재고가 어긋난다.
 * <b>이쪽이 훨씬 위험한 종류의 버그다.</b>
 *
 * <p><b>주의</b>: 개발 DB 에 직접 쓰고 지운다. 테스트가 만든 행만 골라 {@code @AfterEach} 에서
 * 되돌리며, 재고도 원래 값으로 복원한다.
 */
@SpringBootTest(properties = {
        // 스레드마다 커넥션을 하나씩 쥐므로 기본 풀(10)로는 모자란다
        "spring.datasource.hikari.maximum-pool-size=30",
        // 동시 실행 로그가 뒤엉켜 읽기 어려워지므로 SQL 출력을 끈다
        "spring.jpa.properties.hibernate.show_sql=false"
})
@DisplayName("출하 확정 동시성")
class ShipmentConcurrencyTest {

    /** 동시에 확정을 시도할 스레드 수 */
    private static final int THREADS = 10;

    /** 출하 1건당 수량 */
    private static final BigDecimal QTY_PER_SHIPMENT = new BigDecimal("60");

    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;

    @PersistenceContext
    private EntityManager em;

    private TransactionTemplate tx;

    private Long customerId;
    private Long productId;
    private BigDecimal savedOnHand;
    private BigDecimal savedAllocated;

    private final List<Long> createdOrderIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> createdShipmentIds = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger docSeq = new AtomicInteger();
    private long runId;

    @Autowired
    void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        runId = System.currentTimeMillis() % 1_000_000L;

        Customer customer = customerRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CUSTOMER 샘플 데이터가 필요합니다. sample-data.sql 을 먼저 실행하세요."));
        Product product = productRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "PRODUCT 샘플 데이터가 필요합니다. sample-data.sql 을 먼저 실행하세요."));

        customerId = customer.getId();
        productId = product.getId();

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("INVENTORY 행이 없습니다. productId=" + productId));
        savedOnHand = inventory.getOnHandQty();
        savedAllocated = inventory.getAllocatedQty();
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(status -> {
            if (!createdShipmentIds.isEmpty()) {
                em.createNativeQuery("delete from INVENTORY_TXN where REF_TYPE = 'SHIPMENT' and REF_ID in (:ids)")
                        .setParameter("ids", createdShipmentIds)
                        .executeUpdate();
                // SHIPMENT_ITEM 은 FK 가 ON DELETE CASCADE 라 함께 지워진다
                em.createNativeQuery("delete from SHIPMENT where SHIPMENT_ID in (:ids)")
                        .setParameter("ids", createdShipmentIds)
                        .executeUpdate();
            }
            if (!createdOrderIds.isEmpty()) {
                em.createNativeQuery("delete from INVENTORY_TXN where REF_TYPE = 'SALES_ORDER' and REF_ID in (:ids)")
                        .setParameter("ids", createdOrderIds)
                        .executeUpdate();
                em.createNativeQuery("delete from SALES_ORDER where ORDER_ID in (:ids)")
                        .setParameter("ids", createdOrderIds)
                        .executeUpdate();
            }
            em.createNativeQuery("""
                            update INVENTORY
                               set ON_HAND_QTY = :onHand, ALLOCATED_QTY = :allocated
                             where PRODUCT_ID = :productId
                            """)
                    .setParameter("onHand", savedOnHand)
                    .setParameter("allocated", savedAllocated)
                    .setParameter("productId", productId)
                    .executeUpdate();
        });

        createdShipmentIds.clear();
        createdOrderIds.clear();
    }

    // ------------------------------------------------------------------
    // 1. 재고 행 잠금 — 서로 다른 수주가 같은 제품 재고를 놓고 경합한다
    // ------------------------------------------------------------------

    @Test
    @DisplayName("서로 다른 수주 10건을 동시 확정해도 재고가 정확히 차감되고 음수가 되지 않는다")
    void 서로_다른_수주를_동시_출하해도_재고가_정확히_차감된다() throws Exception {
        // given: 재고 300 개. 60 개씩 나가므로 최대 5 건만 성공할 수 있다.
        BigDecimal initialStock = new BigDecimal("300");
        int expectedSuccess = 5;
        setStock(initialStock);

        // 수주를 10건 따로 만든다. 수주 행이 서로 달라야 수주 락에 걸리지 않고
        // 오직 INVENTORY 한 행만 놓고 경합하게 된다.
        List<Long> shipmentIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Long orderId = createOrder(QTY_PER_SHIPMENT);
            shipmentIds.add(createPlannedShipment(orderId, QTY_PER_SHIPMENT));
        }

        // when
        Result result = confirmAllAtOnce(shipmentIds);

        // then
        BigDecimal finalStock = currentStock();

        // 이 단언이 비관적 락의 존재 이유를 잡아낸다.
        // 락을 떼면 @Version 낙관적 락이 대신 걸려 9 건이 ObjectOptimisticLockingFailureException 으로
        // 떨어진다. 데이터는 지켜지지만 사용자는 충돌 에러를 보게 된다.
        assertThat(result.unexpected())
                .as("정상적인 재고 부족 거절 외에 낙관적 락 충돌 같은 예외가 나오면 안 된다")
                .isEmpty();

        assertThat(result.success())
                .as("재고 300 에서 60 씩 나가므로 정확히 5 건만 성공해야 한다")
                .isEqualTo(expectedSuccess);

        assertThat(finalStock)
                .as("재고는 절대 음수가 될 수 없다")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);

        assertThat(finalStock)
                .as("최종 재고 = 초기 재고 - (성공 건수 × 출하량). 어긋나면 갱신 손실이 일어난 것이다")
                .isEqualByComparingTo(
                        initialStock.subtract(QTY_PER_SHIPMENT.multiply(BigDecimal.valueOf(result.success()))));

        assertThat(txnCountFor(shipmentIds))
                .as("재고 이력은 성공한 출하 건수만큼만 남아야 한다 (감사 추적 정합성)")
                .isEqualTo(expectedSuccess);
    }

    // ------------------------------------------------------------------
    // 2. 수주 행 잠금 — 같은 수주의 출하 여러 건을 동시에 확정한다
    // ------------------------------------------------------------------

    @Test
    @DisplayName("같은 수주의 출하 10건을 동시 확정해도 출하수량 누계가 어긋나지 않는다")
    void 같은_수주를_동시_출하해도_출하수량_누계가_어긋나지_않는다() throws Exception {
        // given: 재고는 넉넉하게. 여기서 보려는 건 재고가 아니라 SHIPPED_QTY 누계다.
        setStock(new BigDecimal("100000"));

        BigDecimal orderedQty = QTY_PER_SHIPMENT.multiply(BigDecimal.valueOf(THREADS));   // 600
        Long orderId = createOrder(orderedQty);

        List<Long> shipmentIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            shipmentIds.add(createPlannedShipment(orderId, QTY_PER_SHIPMENT));
        }

        // when
        Result result = confirmAllAtOnce(shipmentIds);

        // then
        assertThat(result.unexpected())
                .as("잔량이 충분하므로 예외 없이 전부 성공해야 한다")
                .isEmpty();
        assertThat(result.success())
                .as("주문량 600 을 60 씩 10 번에 나눠 보내므로 10 건 모두 성공한다")
                .isEqualTo(THREADS);

        OrderSnapshot snapshot = readOrder(orderId);

        // 수주 락을 떼면 여기가 조용히 깨진다. 실측: expected 600, but was 60.
        // 각 트랜잭션이 shippedQty=0 을 따로 읽고 각자 60 을 더해 덮어쓰기 때문이다.
        // SALES_ORDER_ITEM 에는 @Version 이 없어 예외조차 뜨지 않는다.
        assertThat(snapshot.shippedQty())
                .as("출하 누계는 실제로 나간 600 과 정확히 같아야 한다")
                .isEqualByComparingTo(orderedQty);

        assertThat(snapshot.pendingQty())
                .as("전량 출하했으므로 잔량은 0 이다")
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(snapshot.status())
                .as("전량 출하되면 수주 상태가 SHIPPED 로 재판정된다")
                .isEqualTo("SHIPPED");
    }

    // ------------------------------------------------------------------
    // 동시 실행 장치
    // ------------------------------------------------------------------

    /** 동시 실행 결과. 성공 건수와, 재고 부족이 아닌 '예상 밖' 예외 목록을 담는다. */
    private record Result(int success, List<Throwable> insufficientStock, List<Throwable> unexpected) {
    }

    /**
     * 모든 스레드를 출발선에 세운 뒤 한 번에 풀어준다.
     *
     * <p>그냥 submit 만 하면 스레드가 순차적으로 시작돼 경합이 거의 일어나지 않는다.
     * ready/start 두 개의 래치로 "전원 준비 완료 → 동시 출발"을 만들어야
     * 실제로 같은 순간에 같은 행을 노리게 된다.
     */
    private Result confirmAllAtOnce(List<Long> shipmentIds) throws InterruptedException {
        int n = shipmentIds.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);

        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);

        AtomicInteger success = new AtomicInteger();
        List<Throwable> insufficient = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        try {
            for (Long shipmentId : shipmentIds) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        shipmentService.confirm(shipmentId, new ShipmentDtos.ConfirmRequest(
                                "BL-TEST-" + shipmentId, null, LocalDate.now()));
                        success.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        insufficient.add(e);            // 정상적인 거절
                    } catch (Throwable t) {
                        unexpected.add(t);              // 락 타임아웃·데드락 등은 여기로 모인다
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(ready.await(30, TimeUnit.SECONDS))
                    .as("모든 스레드가 출발선에 서야 한다").isTrue();
            start.countDown();

            assertThat(done.await(60, TimeUnit.SECONDS))
                    .as("60초 안에 모두 끝나야 한다. 넘겼다면 데드락을 의심하라").isTrue();
        } finally {
            pool.shutdownNow();
        }

        return new Result(success.get(), insufficient, unexpected);
    }

    // ------------------------------------------------------------------
    // 테스트 데이터
    // ------------------------------------------------------------------

    private Long createOrder(BigDecimal orderedQty) {
        Long id = tx.execute(status -> {
            SalesOrder order = SalesOrder.builder()
                    .orderNo("TST-SO-%d-%d".formatted(runId, docSeq.incrementAndGet()))
                    .customerId(customerId)
                    .orderDate(LocalDate.now())
                    .currency("USD")
                    .incoterms("FOB")
                    .build();
            order.addItem(SalesOrderItem.builder()
                    .lineNo(1)
                    .productId(productId)
                    .orderedQty(orderedQty)
                    .unitPrice(BigDecimal.ONE)
                    .build());
            order.recalcTotal();
            return salesOrderRepository.save(order).getId();
        });
        createdOrderIds.add(id);
        return id;
    }

    private Long createPlannedShipment(Long orderId, BigDecimal qty) {
        Long id = tx.execute(status -> {
            SalesOrder order = salesOrderRepository.findById(orderId).orElseThrow();
            SalesOrderItem orderItem = order.getItems().get(0);

            Shipment shipment = Shipment.builder()
                    .shipmentNo("TST-SH-%d-%d".formatted(runId, docSeq.incrementAndGet()))
                    .orderId(orderId)
                    .transportMode("OCEAN")
                    .etd(LocalDate.now())
                    .containerType("20HQ")
                    .build();
            shipment.addItem(ShipmentItem.builder()
                    .orderItemId(orderItem.getId())
                    .productId(productId)
                    .qty(qty)
                    .build());
            return shipmentRepository.save(shipment).getId();
        });
        createdShipmentIds.add(id);
        return id;
    }

    private void setStock(BigDecimal onHand) {
        tx.executeWithoutResult(status -> em.createNativeQuery("""
                        update INVENTORY
                           set ON_HAND_QTY = :onHand, ALLOCATED_QTY = 0
                         where PRODUCT_ID = :productId
                        """)
                .setParameter("onHand", onHand)
                .setParameter("productId", productId)
                .executeUpdate());
    }

    /** 수주 상태를 트랜잭션 안에서 값으로 떠낸다. 밖으로 엔티티를 들고 나가면 lazy 컬렉션이 터진다. */
    private record OrderSnapshot(BigDecimal shippedQty, BigDecimal pendingQty, String status) {
    }

    private OrderSnapshot readOrder(Long orderId) {
        return tx.execute(status -> {
            SalesOrder order = salesOrderRepository.findById(orderId).orElseThrow();
            SalesOrderItem item = order.getItems().get(0);
            return new OrderSnapshot(item.getShippedQty(), item.getPendingQty(), order.getStatus().name());
        });
    }

    private BigDecimal currentStock() {
        return tx.execute(status -> inventoryRepository.findById(productId).orElseThrow().getOnHandQty());
    }

    private int txnCountFor(List<Long> shipmentIds) {
        Number count = (Number) tx.execute(status -> em.createNativeQuery("""
                        select count(*) from INVENTORY_TXN
                         where REF_TYPE = 'SHIPMENT' and TXN_TYPE = 'OUT' and REF_ID in (:ids)
                        """)
                .setParameter("ids", shipmentIds)
                .getSingleResult());
        return count == null ? 0 : count.intValue();
    }
}
