package com.edu.bootstring.customer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 거래처 API 의 요청/응답 형태
 */
public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record ContactRequest(
            Long id,
            @NotBlank(message = "담당자 이름을 입력해 주세요.") @Size(max = 100) String name,
            @Email(message = "이메일 형식이 올바르지 않습니다.") @Size(max = 150) String email,
            @Size(max = 50) String phone,
            @Size(max = 50) String position,
            /**
             * 대표 연락처 여부. 비우면 false 로 본다.
             *
             * <p>{@code Boolean} 인 것이 의도다 — Boot 4 의 Jackson 3 은 원시 {@code boolean} 에
             * null 이 들어오면 본문 파싱 자체를 실패시킨다.
             */
            Boolean main
    ) {
        public boolean isMainContact() {
            return Boolean.TRUE.equals(main);
        }
    }

    public record ContactResponse(
            Long id,
            String name,
            String email,
            String phone,
            String position,
            boolean main
    ) {
    }

    /** 거래처 등록. 코드는 문서번호에 들어가므로 8자 이내 대문자·숫자로 제한한다. */
    public record CreateRequest(
            @NotBlank(message = "거래처 코드를 입력해 주세요.")
            @Pattern(regexp = "^[A-Z0-9]{2,8}$",
                    message = "거래처 코드는 대문자·숫자 2~8자여야 합니다. 등록 후에는 바꿀 수 없습니다.")
            String customerCode,

            @NotBlank(message = "영문 상호를 입력해 주세요.") @Size(max = 200) String nameEn,
            @Size(max = 200) String nameKo,

            @NotBlank(message = "국가 코드를 입력해 주세요.")
            @Pattern(regexp = "^[A-Z]{2}$", message = "국가 코드는 대문자 2자(ISO)여야 합니다.")
            String countryCode,

            @Size(max = 50) String bizRegNo,
            @Size(max = 500) String address,

            @Pattern(regexp = "^[A-Z]{3}$", message = "통화는 대문자 3자여야 합니다.") String defaultCurrency,
            @Size(max = 10) String defaultIncoterms,
            @Size(max = 20) String paymentTermsCode,

            @Min(value = 0, message = "결제 일수는 0 이상이어야 합니다.")
            @Max(value = 365, message = "결제 일수는 365 이하여야 합니다.")
            Integer paymentDays,

            @DecimalMin(value = "0", message = "선금 비율은 0 이상이어야 합니다.")
            @DecimalMax(value = "100", message = "선금 비율은 100 이하여야 합니다.")
            BigDecimal advanceRate,

            @Min(value = 1, message = "견적 유효일수는 1 이상이어야 합니다.")
            @Max(value = 365, message = "견적 유효일수는 365 이하여야 합니다.")
            Integer quoteValidDays,

            @DecimalMin(value = "0") BigDecimal creditLimit,
            @Size(max = 1000) String remark,

            List<ContactRequest> contacts
    ) {
    }

    /** 수정. {@code customerCode} 가 없는 것이 의도다 — 등록 후 변경 불가 (D2-07). */
    public record UpdateRequest(
            @NotBlank(message = "영문 상호를 입력해 주세요.") @Size(max = 200) String nameEn,
            @Size(max = 200) String nameKo,
            @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "국가 코드는 대문자 2자(ISO)여야 합니다.")
            String countryCode,
            @Size(max = 50) String bizRegNo,
            @Size(max = 500) String address,
            @Pattern(regexp = "^[A-Z]{3}$") String defaultCurrency,
            @Size(max = 10) String defaultIncoterms,
            @Size(max = 20) String paymentTermsCode,
            @Min(0) @Max(365) Integer paymentDays,
            @DecimalMin("0") @DecimalMax("100") BigDecimal advanceRate,
            @Min(1) @Max(365) Integer quoteValidDays,
            @DecimalMin("0") BigDecimal creditLimit,
            @Size(max = 1000) String remark,
            List<ContactRequest> contacts
    ) {
    }

    public record Summary(
            Long id,
            String customerCode,
            String nameEn,
            String nameKo,
            String countryCode,
            String defaultCurrency,
            String defaultIncoterms,
            Integer paymentDays,
            BigDecimal advanceRate,
            Integer quoteValidDays,
            String status,
            String mainContactName,
            /** 거래 이력이 있으면 삭제 대신 비활성만 가능하다 */
            boolean deletable
    ) {
    }

    public record Detail(
            Long id,
            String customerCode,
            String nameEn,
            String nameKo,
            String countryCode,
            String bizRegNo,
            String address,
            String defaultCurrency,
            String defaultIncoterms,
            String paymentTermsCode,
            Integer paymentDays,
            BigDecimal advanceRate,
            Integer quoteValidDays,
            BigDecimal creditLimit,
            String status,
            String remark,
            boolean deletable,
            /** 이 거래처에 걸린 견적·수주 건수 — 삭제 가능 여부의 근거를 화면에 보여준다 */
            long quotationCount,
            long orderCount,
            List<ContactResponse> contacts
    ) {
    }
}
