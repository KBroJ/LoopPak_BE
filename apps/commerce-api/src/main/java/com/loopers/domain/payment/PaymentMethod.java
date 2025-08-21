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
        this.cardNo = Objects.requireNonNull(cardNo, "카드 번호는 필수입니다.");
    }

    public static PaymentMethod of(CardType cardType, String cardNo) {
        return new PaymentMethod(cardType, cardNo);
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
