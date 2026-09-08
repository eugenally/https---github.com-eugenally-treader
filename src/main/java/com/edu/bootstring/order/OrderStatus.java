package com.edu.bootstring.order;

import com.edu.bootstring.global.error.exception.InvalidStateTransitionException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 수주 상태.
 * PARTIALLY_SHIPPED → PARTIALLY_SHIPPED 자기 전이가 허용되는 이유는
 * 부분 출하가 여러 번 반복될 수 있기 때문이다.
 */
public enum OrderStatus {

    CONFIRMED,
    IN_PRODUCTION,
    PARTIALLY_SHIPPED,
    SHIPPED,
    CLOSED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            CONFIRMED, EnumSet.of(IN_PRODUCTION, PARTIALLY_SHIPPED, SHIPPED, CANCELLED),
            IN_PRODUCTION, EnumSet.of(PARTIALLY_SHIPPED, SHIPPED, CANCELLED),
            PARTIALLY_SHIPPED, EnumSet.of(PARTIALLY_SHIPPED, SHIPPED),
            SHIPPED, EnumSet.of(CLOSED),
            CLOSED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public void validateTransitionTo(OrderStatus next) {
        if (this == next) {
            return; // 같은 상태로의 재판정은 변화가 없으므로 통과시킨다
        }
        if (!ALLOWED.getOrDefault(this, Set.of()).contains(next)) {
            throw new InvalidStateTransitionException(
                    "%s → %s 전이는 허용되지 않습니다".formatted(this, next));
        }
    }
}
