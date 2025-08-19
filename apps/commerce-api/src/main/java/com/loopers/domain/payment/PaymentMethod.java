package com.loopers.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod {

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "card_no", nullable = false)
    private String cardNo;

    private PaymentMethod(CardType cardType, String cardNo) {
        this.cardType = Objects.requireNonNull(cardType, "카드 타입은 필수입니다.");
        this.cardNo = maskCardNumber(Objects.requireNonNull(cardNo, "카드 번호는 필수입니다."));
    }

    public static PaymentMethod of(CardType cardType, String cardNo) {
        return new PaymentMethod(cardType, cardNo);
    }

    private String maskCardNumber(String cardNo) {
        if (cardNo == null || cardNo.length() < 4) {
            throw new IllegalArgumentException("유효하지 않은 카드 번호입니다.");
        }

        // 카드 번호 마스킹: 1234-5678-9012-3456 -> 1234-****-****-3456
        String cleanCardNo = cardNo.replaceAll("[^0-9-]", "");
        if (cleanCardNo.length() < 13) {
            throw new IllegalArgumentException("카드 번호가 너무 짧습니다.");
        }

        String[] parts = cleanCardNo.split("-");
        if (parts.length == 4) {
            return parts[0] + "-****-****-" + parts[3];
        }

        // 하이픈 없는 경우 처리
        if (cleanCardNo.length() >= 16) {
            return cleanCardNo.substring(0, 4) + "********" + cleanCardNo.substring(12);
        }

        throw new IllegalArgumentException("유효하지 않은 카드 번호 형식입니다.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentMethod that)) return false;
        return cardType == that.cardType && Objects.equals(cardNo, that.cardNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardType, cardNo);
    }

}
