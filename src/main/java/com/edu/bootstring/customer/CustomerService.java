package com.edu.bootstring.customer;

import com.edu.bootstring.customer.dto.CustomerDtos;
import com.edu.bootstring.global.common.PageResponse;
import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.member.MemberRepository;
import com.edu.bootstring.order.SalesOrderRepository;
import com.edu.bootstring.product.PriceHistoryRepository;
import com.edu.bootstring.quotation.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 거래처 마스터 CRUD.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.Summary> search(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "customerCode"));

        Page<Customer> result = customerRepository.search(
                blankToNull(keyword), blankToNull(status), pageable);

        Map<Long, Long> txnCounts = transactionCounts();
        return PageResponse.from(result, c -> toSummary(c, txnCounts));
    }

    /** 견적·수주 화면의 드롭다운에 쓰는 간단 목록 */
    @Transactional(readOnly = true)
    public List<CustomerDtos.Summary> findActive() {
        Map<Long, Long> txnCounts = transactionCounts();
        return customerRepository.findAllByStatusOrderByNameEnAsc("ACTIVE").stream()
                .map(c -> toSummary(c, txnCounts))
                .toList();
    }

    /** 거래처별 (견적 + 수주) 건수를 두 번의 집계 쿼리로 모은다 */
    private Map<Long, Long> transactionCounts() {
        Map<Long, Long> counts = new HashMap<>();
        quotationRepository.countGroupedByCustomer().forEach(row ->
                counts.merge(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), Long::sum));
        salesOrderRepository.countGroupedByCustomer().forEach(row ->
                counts.merge(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), Long::sum));
        return counts;
    }

    @Transactional(readOnly = true)
    public CustomerDtos.Detail findOne(Long id) {
        return toDetail(get(id));
    }

    @Transactional
    public CustomerDtos.Detail create(CustomerDtos.CreateRequest req) {
        if (customerRepository.existsByCustomerCode(req.customerCode())) {
            throw new BusinessException("이미 사용 중인 거래처 코드입니다: " + req.customerCode(),
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        Customer customer = Customer.builder()
                .customerCode(req.customerCode())
                .nameEn(req.nameEn())
                .nameKo(req.nameKo())
                .countryCode(req.countryCode())
                .bizRegNo(req.bizRegNo())
                .address(req.address())
                .defaultCurrency(req.defaultCurrency())
                .defaultIncoterms(req.defaultIncoterms())
                .paymentTermsCode(req.paymentTermsCode())
                .paymentDays(req.paymentDays())
                .advanceRate(req.advanceRate())
                .quoteValidDays(req.quoteValidDays())
                .creditLimit(req.creditLimit())
                .remark(req.remark())
                .build();

        applyContacts(customer, req.contacts());

        return toDetail(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDtos.Detail update(Long id, CustomerDtos.UpdateRequest req) {
        Customer customer = get(id);

        customer.update(req.nameEn(), req.nameKo(), req.countryCode(), req.bizRegNo(),
                req.address(), req.defaultCurrency(), req.defaultIncoterms(),
                req.paymentTermsCode(), req.paymentDays(), req.advanceRate(),
                req.quoteValidDays(), req.creditLimit(), req.remark());

        syncContacts(customer, req.contacts());

        return toDetail(customer);
    }

    /**
     * 삭제.
     *
     * <p>거래 이력이 있으면 물리 삭제하지 않는다. 견적·수주가 이 거래처를 FK 로 가리키고 있어
     * 지우면 과거 문서가 깨진다. 대신 {@code INACTIVE} 로 돌려 새 거래에서만 빠지게 한다.
     *
     * <p>연결된 거래처 계정(ROLE_CUSTOMER)이 있어도 지우지 않는다.
     * 사람 계정이 붙은 마스터를 지우면 그 사람이 로그인할 수 없게 된다.
     */
    @Transactional
    public String delete(Long id) {
        Customer customer = get(id);

        if (hasTransactions(customer.getId())) {
            customer.deactivate();
            return "거래 이력이 있어 삭제하지 않고 비활성 처리했습니다.";
        }
        if (memberRepository.countByCustomerId(customer.getId()) > 0) {
            customer.deactivate();
            return "연결된 회원 계정이 있어 삭제하지 않고 비활성 처리했습니다.";
        }

        // 단가는 이 거래처에만 딸린 것이라 함께 지운다. 담당자는 cascade 로 따라간다.
        priceHistoryRepository.deleteAll(priceHistoryRepository.findAllByCustomerId(customer.getId()));
        customerRepository.delete(customer);
        return "거래처를 삭제했습니다.";
    }

    @Transactional
    public CustomerDtos.Detail changeStatus(Long id, boolean active) {
        Customer customer = get(id);
        if (active) {
            customer.activate();
        } else {
            customer.deactivate();
        }
        return toDetail(customer);
    }

    // ------------------------------------------------------------------
    // 담당자
    // ------------------------------------------------------------------

    private void applyContacts(Customer customer, List<CustomerDtos.ContactRequest> requests) {
        if (requests == null) {
            return;
        }
        requests.forEach(c -> customer.addContact(CustomerContact.builder()
                .name(c.name())
                .email(c.email())
                .phone(c.phone())
                .position(c.position())
                .main(c.isMainContact())
                .build()));
    }

    /**
     * 담당자 목록을 요청 내용에 맞춘다.
     * id 가 있으면 수정, 없으면 추가, 요청에 빠진 기존 행은 삭제한다.
     */
    private void syncContacts(Customer customer, List<CustomerDtos.ContactRequest> requests) {
        if (requests == null) {
            return;
        }

        List<Long> keepIds = requests.stream()
                .map(CustomerDtos.ContactRequest::id)
                .filter(Objects::nonNull)
                .toList();

        List<CustomerContact> removed = customer.getContacts().stream()
                .filter(c -> !keepIds.contains(c.getId()))
                .toList();
        removed.forEach(customer::removeContact);

        for (CustomerDtos.ContactRequest req : requests) {
            if (req.id() == null) {
                customer.addContact(CustomerContact.builder()
                        .name(req.name()).email(req.email()).phone(req.phone())
                        .position(req.position()).main(req.isMainContact())
                        .build());
            } else {
                customer.getContacts().stream()
                        .filter(c -> req.id().equals(c.getId()))
                        .findFirst()
                        .ifPresent(c -> c.update(req.name(), req.email(), req.phone(), req.position()));
            }
        }

        // 대표 연락처는 하나만 남긴다
        requests.stream()
                .filter(CustomerDtos.ContactRequest::isMainContact)
                .filter(r -> r.id() != null)
                .findFirst()
                .flatMap(r -> customer.getContacts().stream()
                        .filter(c -> r.id().equals(c.getId())).findFirst())
                .ifPresent(customer::makeSoleMain);
    }

    // ------------------------------------------------------------------
    // 내부
    // ------------------------------------------------------------------

    private boolean hasTransactions(Long customerId) {
        return quotationRepository.countByCustomerId(customerId) > 0
                || salesOrderRepository.countByCustomerId(customerId) > 0;
    }

    Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("거래처를 찾을 수 없습니다. id=" + id));
    }

    private CustomerDtos.Summary toSummary(Customer c, Map<Long, Long> txnCounts) {
        CustomerContact main = c.mainContact();
        return new CustomerDtos.Summary(
                c.getId(), c.getCustomerCode(), c.getNameEn(), c.getNameKo(), c.getCountryCode(),
                c.getDefaultCurrency(), c.getDefaultIncoterms(), c.getPaymentDays(),
                c.getAdvanceRate(), c.getQuoteValidDays(), c.getStatus(),
                main != null ? main.getName() : null,
                txnCounts.getOrDefault(c.getId(), 0L) == 0L
                        && memberRepository.countByCustomerId(c.getId()) == 0);
    }

    private CustomerDtos.Detail toDetail(Customer c) {
        long quotationCount = quotationRepository.countByCustomerId(c.getId());
        long orderCount = salesOrderRepository.countByCustomerId(c.getId());

        List<CustomerDtos.ContactResponse> contacts = c.getContacts().stream()
                .map(ct -> new CustomerDtos.ContactResponse(
                        ct.getId(), ct.getName(), ct.getEmail(), ct.getPhone(),
                        ct.getPosition(), ct.isMain()))
                .toList();

        return new CustomerDtos.Detail(
                c.getId(), c.getCustomerCode(), c.getNameEn(), c.getNameKo(), c.getCountryCode(),
                c.getBizRegNo(), c.getAddress(), c.getDefaultCurrency(), c.getDefaultIncoterms(),
                c.getPaymentTermsCode(), c.getPaymentDays(), c.getAdvanceRate(),
                c.getQuoteValidDays(), c.getCreditLimit(), c.getStatus(), c.getRemark(),
                quotationCount == 0 && orderCount == 0
                        && memberRepository.countByCustomerId(c.getId()) == 0,
                quotationCount, orderCount, contacts);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
