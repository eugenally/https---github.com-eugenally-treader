package com.edu.bootstring.customer;

import com.edu.bootstring.customer.dto.CustomerDtos;
import com.edu.bootstring.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    /** 목록 — 페이징·검색. 견적 화면의 드롭다운은 {@code /active} 를 쓴다. */
    @GetMapping
    public PageResponse<CustomerDtos.Summary> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return customerService.search(page, size, keyword, status);
    }

    /** 활성 거래처 전체 — 선택 드롭다운용 */
    @GetMapping("/active")
    public List<CustomerDtos.Summary> findActive() {
        return customerService.findActive();
    }

    @GetMapping("/{id}")
    public CustomerDtos.Detail findOne(@PathVariable Long id) {
        return customerService.findOne(id);
    }

    @PostMapping
    public CustomerDtos.Detail create(@Valid @RequestBody CustomerDtos.CreateRequest req) {
        return customerService.create(req);
    }

    @PutMapping("/{id}")
    public CustomerDtos.Detail update(@PathVariable Long id,
                                      @Valid @RequestBody CustomerDtos.UpdateRequest req) {
        return customerService.update(id, req);
    }

    /** 거래 이력이 있으면 삭제 대신 비활성 처리하고, 어느 쪽이었는지 메시지로 알려준다 */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        return Map.of("message", customerService.delete(id));
    }

    @PostMapping("/{id}/status")
    public CustomerDtos.Detail changeStatus(@PathVariable Long id,
                                            @RequestParam boolean active) {
        return customerService.changeStatus(id, active);
    }
}
