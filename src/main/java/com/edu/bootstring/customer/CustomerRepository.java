package com.edu.bootstring.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByStatusOrderByNameEnAsc(String status);

    boolean existsByCustomerCode(String customerCode);

    /** 코드·영문명·한글명 어디에 걸려도 찾아 준다. 상태 필터는 비우면 전체다. */
    @Query("""
            select c from Customer c
             where (:status is null or c.status = :status)
               and (:keyword is null
                    or lower(c.customerCode) like lower(concat('%', :keyword, '%'))
                    or lower(c.nameEn)       like lower(concat('%', :keyword, '%'))
                    or lower(c.nameKo)       like lower(concat('%', :keyword, '%')))
            """)
    Page<Customer> search(@Param("keyword") String keyword,
                          @Param("status") String status,
                          Pageable pageable);
}
