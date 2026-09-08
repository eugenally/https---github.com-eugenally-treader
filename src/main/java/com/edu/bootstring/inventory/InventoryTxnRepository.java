package com.edu.bootstring.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTxnRepository extends JpaRepository<InventoryTxn, Long> {

    /**
     * 제품의 재고 변동 이력 건수.
     *
     * <p>이력이 있으면 제품을 물리 삭제하지 않는다. 이 행들은 <b>감사 추적</b>이라
     * 제품을 지우자고 함께 지울 성격이 아니다.
     */
    long countByProductId(Long productId);
}
