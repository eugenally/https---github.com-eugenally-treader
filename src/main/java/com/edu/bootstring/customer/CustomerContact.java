package com.edu.bootstring.customer;

import com.edu.bootstring.global.common.YesNoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래처 담당자. 한 거래처에 여럿이 붙는다 (1:N).
 *
 * <p>실무에서 구매 담당과 물류 담당이 다른 경우가 흔하다.
 * {@code main} 이 대표 연락처이고, 인보이스·알림은 여기로 나간다.
 */
@Entity
@Getter
@Table(name = "CUSTOMER_CONTACT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONTACT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "PHONE", length = 50)
    private String phone;

    @Column(name = "POSITION", length = 50)
    private String position;

    /** 대표 연락처 여부. 거래처당 하나만 Y 가 되도록 서비스가 관리한다. */
    @Convert(converter = YesNoConverter.class)
    @Column(name = "MAIN_YN", nullable = false, length = 1)
    private boolean main;

    @Builder
    private CustomerContact(String name, String email, String phone, String position, boolean main) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.main = main;
    }

    void assignTo(Customer customer) {
        this.customer = customer;
    }

    public void update(String name, String email, String phone, String position) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.position = position;
    }

    public void markMain(boolean main) {
        this.main = main;
    }
}
