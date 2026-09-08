package com.edu.bootstring.global.docnumber;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocNumberRepository extends JpaRepository<DocNumber, DocNumberId> {

    /** 채번 카운터 행을 잠근다. 같은 번호가 두 번 나가는 것을 막는 유일한 수단이다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from DocNumber d
             where d.docType = :docType
               and d.customerCode = :customerCode
               and d.yearVal = :yearVal
            """)
    Optional<DocNumber> findForUpdate(@Param("docType") String docType,
                                      @Param("customerCode") String customerCode,
                                      @Param("yearVal") Integer yearVal);
}
