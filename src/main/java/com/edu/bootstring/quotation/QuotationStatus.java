package com.edu.bootstring.quotation;

import com.edu.bootstring.global.error.exception.InvalidStateTransitionException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 견적 상태. 전이 규칙을 여기 한 곳에만 둔다.
 * 서비스 코드에 상태 비교문을 흩뿌리면 조건 하나를 반드시 빠뜨린다.
 */
public enum QuotationStatus {

    DRAFT,
    SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    /** 개정판(Rev+1) 발행 시 이전 건이 자동으로 넘어가는 상태 — 사람이 직접 누르지 않는다 */
    SUPERSEDED;

    private static final Map<QuotationStatus, Set<QuotationStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(SENT),
            SENT, EnumSet.of(DRAFT, ACCEPTED, REJECTED, EXPIRED, SUPERSEDED),
            ACCEPTED, EnumSet.noneOf(QuotationStatus.class),
            REJECTED, EnumSet.noneOf(QuotationStatus.class),
            EXPIRED, EnumSet.noneOf(QuotationStatus.class),
            SUPERSEDED, EnumSet.noneOf(QuotationStatus.class)
    );

    public void validateTransitionTo(QuotationStatus next) {
        if (!ALLOWED.getOrDefault(this, Set.of()).contains(next)) {
            throw new InvalidStateTransitionException(
                    "%s → %s 전이는 허용되지 않습니다".formatted(this, next));
        }
    }
}
