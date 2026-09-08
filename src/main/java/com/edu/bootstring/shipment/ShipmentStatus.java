package com.edu.bootstring.shipment;

import com.edu.bootstring.global.error.exception.InvalidStateTransitionException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 출하 상태.
 * 재고는 SHIPPED 전환 시점에만 차감된다 (D5-08).
 * PLANNED 는 삭제 가능한 상태이므로 그 단계에서 재고를 건드리면 정합성이 깨진다.
 */
public enum ShipmentStatus {

    PLANNED,
    PACKED,
    SHIPPED,
    ARRIVED,
    CANCELLED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = Map.of(
            PLANNED, EnumSet.of(PACKED, SHIPPED, CANCELLED),
            PACKED, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(ARRIVED),
            ARRIVED, EnumSet.noneOf(ShipmentStatus.class),
            CANCELLED, EnumSet.noneOf(ShipmentStatus.class)
    );

    public void validateTransitionTo(ShipmentStatus next) {
        if (!ALLOWED.getOrDefault(this, Set.of()).contains(next)) {
            throw new InvalidStateTransitionException(
                    "%s → %s 전이는 허용되지 않습니다".formatted(this, next));
        }
    }
}
