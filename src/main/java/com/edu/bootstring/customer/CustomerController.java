package com.edu.bootstring.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public record CustomerResponse(
            Long id,
            String customerCode,
            String nameEn,
            String nameKo,
            String countryCode,
            String defaultCurrency,
            String defaultIncoterms,
            Integer quoteValidDays,
            Integer paymentDays,
            BigDecimal advanceRate
    ) {
    }

    @GetMapping
    public List<CustomerResponse> findAll() {
        return customerRepository.findAllByStatusOrderByNameEnAsc("ACTIVE").stream()
                .map(c -> new CustomerResponse(
                        c.getId(),
                        c.getCustomerCode(),
                        c.getNameEn(),
                        c.getNameKo(),
                        c.getCountryCode(),
                        c.getDefaultCurrency(),
                        c.getDefaultIncoterms(),
                        c.getQuoteValidDays(),
                        c.getPaymentDays(),
                        c.getAdvanceRate()))
                .toList();
    }
}
