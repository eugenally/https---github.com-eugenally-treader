package com.edu.bootstring.shipment;

import com.edu.bootstring.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 출하 1건. 수주 1건에 여러 출하가 달릴 수 있다(부분 선적).
 *
 * <p>DDL 의 CK_SHIPMENT_DOC 제약 때문에 SHIPPED 로 넘어가려면
 * 해상은 B/L 번호, 항공은 AWB 번호가 반드시 있어야 한다.
 */
@Entity
@Getter
@Table(name = "SHIPMENT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqShipment")
    @SequenceGenerator(name = "seqShipment", sequenceName = "SEQ_SHIPMENT", allocationSize = 1)
    @Column(name = "SHIPMENT_ID")
    private Long id;

    @Column(name = "SHIPMENT_NO", nullable = false, length = 40, updatable = false)
    private String shipmentNo;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    /** OCEAN / AIR */
    @Column(name = "TRANSPORT_MODE", nullable = false, length = 10)
    private String transportMode;

    @Column(name = "SHIP_DATE")
    private LocalDate shipDate;

    @Column(name = "ETD")
    private LocalDate etd;

    @Column(name = "ETA")
    private LocalDate eta;

    @Column(name = "VESSEL_NAME", length = 100)
    private String vesselName;

    /** 해상 선하증권 번호 — CI 결제기한 기산일의 근거가 된다 (D5-11) */
    @Column(name = "BL_NO", length = 50)
    private String blNo;

    @Column(name = "CONTAINER_NO", length = 50)
    private String containerNo;

    @Column(name = "CONTAINER_TYPE", length = 20)
    private String containerType;

    @Column(name = "AWB_NO", length = 50)
    private String awbNo;

    @Column(name = "FLIGHT_NO", length = 50)
    private String flightNo;

    @Column(name = "PORT_OF_LOADING", length = 100)
    private String portOfLoading;

    @Column(name = "PORT_OF_DISCHARGE", length = 100)
    private String portOfDischarge;

    @Column(name = "TOTAL_NET_WEIGHT", precision = 15, scale = 3)
    private BigDecimal totalNetWeight;

    @Column(name = "TOTAL_GROSS_WEIGHT", precision = 15, scale = 3)
    private BigDecimal totalGrossWeight;

    @Column(name = "TOTAL_CBM", precision = 15, scale = 4)
    private BigDecimal totalCbm;

    @Column(name = "TOTAL_CARTON")
    private Integer totalCarton;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(name = "REMARK", length = 1000)
    private String remark;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentItem> items = new ArrayList<>();

    @Builder
    private Shipment(String shipmentNo, Long orderId, String transportMode, LocalDate etd, LocalDate eta,
                     String vesselName, String blNo, String containerNo, String containerType,
                     String awbNo, String flightNo, String portOfLoading, String portOfDischarge,
                     BigDecimal totalCbm, Integer totalCarton, String remark) {
        this.shipmentNo = shipmentNo;
        this.orderId = orderId;
        this.transportMode = transportMode == null ? "OCEAN" : transportMode;
        this.etd = etd;
        this.eta = eta;
        this.vesselName = vesselName;
        this.blNo = blNo;
        this.containerNo = containerNo;
        this.containerType = containerType;
        this.awbNo = awbNo;
        this.flightNo = flightNo;
        this.portOfLoading = portOfLoading;
        this.portOfDischarge = portOfDischarge;
        this.totalCbm = totalCbm;
        this.totalCarton = totalCarton;
        this.remark = remark;
        this.status = ShipmentStatus.PLANNED;
    }

    public void addItem(ShipmentItem item) {
        item.assignTo(this);
        this.items.add(item);
    }

    public void changeStatus(ShipmentStatus next) {
        this.status.validateTransitionTo(next);
        this.status = next;
    }

    /** B/L 번호는 확정 시점에 들어온다. 선적 전에는 아직 발급되지 않기 때문이다. */
    public void assignBlNo(String blNo) {
        this.blNo = blNo;
    }

    public void assignAwbNo(String awbNo) {
        this.awbNo = awbNo;
    }

    public void markShipped(LocalDate shipDate) {
        changeStatus(ShipmentStatus.SHIPPED);
        this.shipDate = shipDate;
    }

    /** 품목 중량 합계를 헤더에 반영한다. 실측치가 다르면 사용자가 덮어쓸 수 있다. */
    public void recalcWeights() {
        this.totalNetWeight = sum(ShipmentItem::getNetWeight);
        this.totalGrossWeight = sum(ShipmentItem::getGrossWeight);
    }

    private BigDecimal sum(java.util.function.Function<ShipmentItem, BigDecimal> field) {
        return items.stream()
                .map(field)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** PLANNED 인 출하만 삭제할 수 있다 (D5-08). 재고를 아직 건드리지 않았기 때문이다. */
    public boolean isDeletable() {
        return status == ShipmentStatus.PLANNED;
    }

    /** SHIPPED 전환에 필요한 운송서류 번호가 채워져 있는지 */
    public boolean hasTransportDocNo() {
        return "AIR".equals(transportMode)
                ? awbNo != null && !awbNo.isBlank()
                : blNo != null && !blNo.isBlank();
    }
}
