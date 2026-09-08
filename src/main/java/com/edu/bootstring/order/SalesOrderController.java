package com.edu.bootstring.order;

import com.edu.bootstring.order.dto.OrderDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    public List<OrderDtos.SummaryResponse> findAll() {
        return salesOrderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderDtos.DetailResponse findOne(@PathVariable Long id) {
        return salesOrderService.findDetail(id);
    }

    /** 출하 등록 화면이 쓰는 미출하 잔량 목록 */
    @GetMapping("/{id}/pending-items")
    public List<OrderDtos.PendingItemResponse> findPendingItems(@PathVariable Long id) {
        return salesOrderService.findPendingItems(id);
    }
}
