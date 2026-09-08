package com.edu.bootstring.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    Optional<Member> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    /** 이메일 변경 시 자기 자신은 중복에서 제외해야 한다 */
    boolean existsByEmailAndIdNot(String email, Long id);

    List<Member> findAllByOrderByIdDesc();

    /** 거래처 삭제 가능 여부 판단에 쓴다 (ROLE_CUSTOMER 계정이 붙어 있는지) */
    long countByCustomerId(Long customerId);
}
