package com.edu.bootstring.inventory;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 제품별 재고. 창고는 1개로 가정한다.
 *
 * <p>재고 변동 시점은 D5-04 / D5-08 결정을 따른다.
 * <ul>
 *   <li>수주 확정 → {@code allocatedQty} 증가 (실물은 그대로)</li>
 *   <li>출하 SHIPPED 전환 → {@code onHandQty}, {@code allocatedQty} 동시 감소</li>
 * </ul>
 * PLANNED 출하는 삭제 가능해야 하므로 그 단계에서는 재고를 건드리지 않는다.
 */
@Entity
@Getter
@Table(name = "INVENTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    /** PRODUCT_ID 가 곧 PK다 (창고 1개 가정) */
    @Id
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "ON_HAND_QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal onHandQty;

    @Column(name = "ALLOCATED_QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal allocatedQty;

    @Column(name = "SAFETY_QTY", nullable = false, precision = 15, scale = 3)
    private BigDecimal safetyQty;

    /** 낙관적 락 — 재고 차감은 비관적 락을 쓰지만, 두 방식을 비교하기 위해 컬럼은 유지한다 */
    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    /** 가용 재고. D5-05 로 음수가 될 수 있고, 음수는 곧 백오더 수량이다. */
    public BigDecimal getAvailableQty() {
        return onHandQty.subtract(allocatedQty);
    }

    /** 수주 확정 시 할당 */
    public void allocate(BigDecimal qty) {
        this.allocatedQty = this.allocatedQty.add(qty);
        this.updatedAt = LocalDateTime.now();
    }

    /** 수주 취소 시 할당 해제 */
    public void release(BigDecimal qty) {
        BigDecimal next = this.allocatedQty.subtract(qty);
        if (next.signum() < 0) {
            throw new BusinessException("할당 수량보다 많이 해제할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        this.allocatedQty = next;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 출하 확정 시 실물 재고 차감.
     * 반드시 비관적 락으로 행을 잠근 뒤 호출해야 Lost Update 가 나지 않는다.
     */
    public void deduct(BigDecimal qty) {
        if (this.onHandQty.compareTo(qty) < 0) {
            throw new InsufficientStockException(
                    "재고 부족: 현재 %s, 요청 %s".formatted(this.onHandQty, qty));
        }
        this.onHandQty = this.onHandQty.subtract(qty);
        // 할당분에서도 빼되, 할당보다 많이 출하하는 경우(직접 출하)에는 0 에서 멈춘다
        this.allocatedQty = this.allocatedQty.subtract(qty).max(BigDecimal.ZERO);
        this.updatedAt = LocalDateTime.now();
    }
}
