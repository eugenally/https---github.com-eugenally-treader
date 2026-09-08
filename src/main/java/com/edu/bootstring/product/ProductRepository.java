package com.edu.bootstring.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByStatusOrderByProductCodeAsc(String status);

    boolean existsByProductCode(String productCode);

    @Query("""
            select p from Product p
             where (:status is null or p.status = :status)
               and (:keyword is null
                    or lower(p.productCode) like lower(concat('%', :keyword, '%'))
                    or lower(p.nameEn)      like lower(concat('%', :keyword, '%'))
                    or lower(p.nameKo)      like lower(concat('%', :keyword, '%'))
                    or lower(p.hsCode)      like lower(concat('%', :keyword, '%')))
            """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("status") String status,
                         Pageable pageable);
}
