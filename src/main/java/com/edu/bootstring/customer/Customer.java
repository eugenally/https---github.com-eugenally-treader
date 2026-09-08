package com.edu.bootstring.customer;

import com.edu.bootstring.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 거래처 마스터
 */
@Entity
@Getter
@Table(name = "CUSTOMER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCustomer")
    @SequenceGenerator(name = "seqCustomer", sequenceName = "SEQ_CUSTOMER", allocationSize = 1)
    @Column(name = "CUSTOMER_ID")
    private Long id;

    @Column(name = "CUSTOMER_CODE", nullable = false, length = 8, updatable = false)
    private String customerCode;

    @Column(name = "NAME_EN", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "NAME_KO", length = 200)
    private String nameKo;

    @Column(name = "COUNTRY_CODE", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "BIZ_REG_NO", length = 50)
    private String bizRegNo;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "DEFAULT_CURRENCY", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "DEFAULT_INCOTERMS", length = 10)
    private String defaultIncoterms;

    @Column(name = "PAYMENT_TERMS_CODE", nullable = false, length = 20)
    private String paymentTermsCode;

    @Column(name = "PAYMENT_DAYS")
    private Integer paymentDays;

    /** 선금 비율(%) — 견적 단계 PI 금액 산정에 쓰인다 (D5-09) */
    @Column(name = "ADVANCE_RATE", precision = 5, scale = 2)
    private BigDecimal advanceRate;

    /** 견적 유효기한 기본 일수 (D5-01) */
    @Column(name = "QUOTE_VALID_DAYS", nullable = false)
    private Integer quoteValidDays;

    @Column(name = "CREDIT_LIMIT", precision = 18, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "REMARK", length = 1000)
    private String remark;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<CustomerContact> contacts = new ArrayList<>();

    @Builder
    private Customer(String customerCode, String nameEn, String nameKo, String countryCode,
                     String bizRegNo, String address, String defaultCurrency, String defaultIncoterms,
                     String paymentTermsCode, Integer paymentDays, BigDecimal advanceRate,
                     Integer quoteValidDays, BigDecimal creditLimit, String remark) {
        this.customerCode = customerCode;
        this.nameEn = nameEn;
        this.nameKo = nameKo;
        this.countryCode = countryCode;
        this.bizRegNo = bizRegNo;
        this.address = address;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "USD";
        this.defaultIncoterms = defaultIncoterms;
        this.paymentTermsCode = paymentTermsCode != null ? paymentTermsCode : "TT_30";
        this.paymentDays = paymentDays;
        this.advanceRate = advanceRate;
        this.quoteValidDays = quoteValidDays != null ? quoteValidDays : 30;
        this.creditLimit = creditLimit;
        this.remark = remark;
        this.status = "ACTIVE";
    }

    /**
     * 거래처 정보 수정.
     *
     * <p>{@code customerCode} 는 여기 없다. 문서번호(QT-ABC-2026-0001)에 박혀 나가므로
     * 등록 후 바꾸면 과거 문서와 어긋난다 (D2-07).
     */
    public void update(String nameEn, String nameKo, String countryCode, String bizRegNo,
                       String address, String defaultCurrency, String defaultIncoterms,
                       String paymentTermsCode, Integer paymentDays, BigDecimal advanceRate,
                       Integer quoteValidDays, BigDecimal creditLimit, String remark) {
        this.nameEn = nameEn;
        this.nameKo = nameKo;
        this.countryCode = countryCode;
        this.bizRegNo = bizRegNo;
        this.address = address;
        this.defaultCurrency = defaultCurrency;
        this.defaultIncoterms = defaultIncoterms;
        this.paymentTermsCode = paymentTermsCode;
        this.paymentDays = paymentDays;
        this.advanceRate = advanceRate;
        this.quoteValidDays = quoteValidDays;
        this.creditLimit = creditLimit;
        this.remark = remark;
    }

    /** 거래 이력이 있는 거래처는 지우지 않고 비활성으로 돌린다 */
    public void deactivate() {
        this.status = "INACTIVE";
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    // ------------------------------------------------------------------
    // 담당자
    // ------------------------------------------------------------------

    public void addContact(CustomerContact contact) {
        contact.assignTo(this);
        this.contacts.add(contact);
        // 첫 담당자는 자동으로 대표가 된다
        if (this.contacts.size() == 1) {
            contact.markMain(true);
        } else if (contact.isMain()) {
            makeSoleMain(contact);
        }
    }

    public void removeContact(CustomerContact contact) {
        this.contacts.remove(contact);
        // 대표를 지웠으면 남은 첫 담당자를 대표로 올린다
        if (contact.isMain() && !this.contacts.isEmpty()) {
            makeSoleMain(this.contacts.get(0));
        }
    }

    /** 대표 연락처는 거래처당 하나뿐이어야 한다 */
    public void makeSoleMain(CustomerContact target) {
        this.contacts.forEach(c -> c.markMain(c == target));
    }

    public CustomerContact mainContact() {
        return contacts.stream().filter(CustomerContact::isMain).findFirst()
                .orElse(contacts.isEmpty() ? null : contacts.get(0));
    }
}
