package com.edu.bootstring.quotation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    /** 제품 삭제 가능 여부 판단에 쓴다 */
    long countByProductId(Long productId);
}
