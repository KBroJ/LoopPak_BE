package com.loopers.domain.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @DisplayName("Payment 생성")
    @Nested
    class CreatePayment {

        @DisplayName("정상적인 정보로 Payment를 생성한다.")
        @Test
        void createPayment_withValidInfo() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;

            // act
            Payment payment = Payment.of(orderId, paymentMethod, amount);

            // assert
            assertThat(payment.getOrderId()).isEqualTo(orderId);
            assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
            assertThat(payment.getAmount()).isEqualTo(amount);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getTransactionKey()).isNull();
        }

        @DisplayName("주문 ID가 null일 경우 IllegalArgumentException이 발생한다.")
        @Test
        void createPayment_throwsException_whenOrderIdIsNull() {
            // arrange
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;

            // act & assert
            assertThatThrownBy(() -> Payment.of(null, paymentMethod, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 주문 ID입니다.");
        }

        @DisplayName("주문 ID가 0 이하일 경우 IllegalArgumentException이 발생한다.")
        @Test
        void createPayment_throwsException_whenOrderIdIsZeroOrNegative() {
            // arrange
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;

            // act & assert
            assertThatThrownBy(() -> Payment.of(0L, paymentMethod, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 주문 ID입니다.");

            assertThatThrownBy(() -> Payment.of(-1L, paymentMethod, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 주문 ID입니다.");
        }

        @DisplayName("결제 수단이 null일 경우 IllegalArgumentException이 발생한다.")
        @Test
        void createPayment_throwsException_whenPaymentMethodIsNull() {
            // arrange
            Long orderId = 1L;
            long amount = 10000L;

            // act & assert
            assertThatThrownBy(() -> Payment.of(orderId, null, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결제 수단은 필수입니다.");
        }

        @DisplayName("결제 금액이 0 이하일 경우 IllegalArgumentException이 발생한다.")
        @Test
        void createPayment_throwsException_whenAmountIsZeroOrNegative() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");

            // act & assert
            assertThatThrownBy(() -> Payment.of(orderId, paymentMethod, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결제 금액은 0보다 커야 합니다.");

            assertThatThrownBy(() -> Payment.of(orderId, paymentMethod, -1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결제 금액은 0보다 커야 합니다.");
        }
    }

    @DisplayName("Transaction Key 업데이트")
    @Nested
    class UpdateTransactionKey {

        @DisplayName("Transaction Key를 정상적으로 업데이트한다.")
        @Test
        void updateTransactionKey_updatesSuccessfully() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);
            String transactionKey = "20250822:TR:abc123";

            // act
            payment.updateTransactionKey(transactionKey);

            // assert
            assertThat(payment.getTransactionKey()).isEqualTo(transactionKey);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING); // 상태는 변경되지 않음
        }

        @DisplayName("이미 Transaction Key가 있는 경우에도 업데이트가 가능하다.")
        @Test
        void updateTransactionKey_canUpdateExistingKey() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);
            String oldTransactionKey = "20250822:TR:old123";
            String newTransactionKey = "20250822:TR:new456";

            payment.updateTransactionKey(oldTransactionKey);

            // act
            payment.updateTransactionKey(newTransactionKey);

            // assert
            assertThat(payment.getTransactionKey()).isEqualTo(newTransactionKey);
        }
    }

    @DisplayName("결제 성공 처리")
    @Nested
    class MarkAsSuccess {

        @DisplayName("결제를 성공 상태로 변경한다.")
        @Test
        void markAsSuccess_changesStatusToSuccess() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);
            String transactionKey = "20250822:TR:success123";

            // act
            payment.markAsSuccess(transactionKey);

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getTransactionKey()).isEqualTo(transactionKey);
            assertThat(payment.isSuccess()).isTrue();
            assertThat(payment.isPending()).isFalse();
        }

        @DisplayName("기존 Transaction Key가 있어도 새로운 값으로 업데이트된다.")
        @Test
        void markAsSuccess_updatesTransactionKeyEvenIfExists() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);
            payment.updateTransactionKey("20250822:TR:old123");

            String newTransactionKey = "20250822:TR:success456";

            // act
            payment.markAsSuccess(newTransactionKey);

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getTransactionKey()).isEqualTo(newTransactionKey);
        }
    }

    @DisplayName("결제 실패 처리")
    @Nested
    class MarkAsFailed {

        @DisplayName("결제를 실패 상태로 변경한다.")
        @Test
        void markAsFailed_changesStatusToFailed() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);

            // act
            payment.markAsFailed();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.isSuccess()).isFalse();
            assertThat(payment.isPending()).isFalse();
        }

        @DisplayName("Transaction Key는 변경되지 않는다.")
        @Test
        void markAsFailed_preservesTransactionKey() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);
            String originalTransactionKey = "20250822:TR:failed123";
            payment.updateTransactionKey(originalTransactionKey);

            // act
            payment.markAsFailed();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getTransactionKey()).isEqualTo(originalTransactionKey);
        }
    }

    @DisplayName("결제 상태 확인")
    @Nested
    class CheckStatus {

        @DisplayName("PENDING 상태 확인이 정상적으로 동작한다.")
        @Test
        void isPending_returnsTrueForPendingStatus() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);

            // act & assert
            assertThat(payment.isPending()).isTrue();
            assertThat(payment.isSuccess()).isFalse();
        }

        @DisplayName("SUCCESS 상태 확인이 정상적으로 동작한다.")
        @Test
        void isSuccess_returnsTrueForSuccessStatus() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);

            // act
            payment.markAsSuccess("20250822:TR:success123");

            // assert
            assertThat(payment.isSuccess()).isTrue();
            assertThat(payment.isPending()).isFalse();
        }

        @DisplayName("FAILED 상태에서는 모든 확인 메서드가 false를 반환한다.")
        @Test
        void statusCheck_returnsFalseForFailedStatus() {
            // arrange
            Long orderId = 1L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");
            long amount = 10000L;
            Payment payment = Payment.of(orderId, paymentMethod, amount);

            // act
            payment.markAsFailed();

            // assert
            assertThat(payment.isSuccess()).isFalse();
            assertThat(payment.isPending()).isFalse();
        }
    }

    @DisplayName("PaymentMethod 생성 및 검증")
    @Nested
    class PaymentMethodCreation {

        @DisplayName("정상적인 카드 정보로 PaymentMethod를 생성한다.")
        @Test
        void createPaymentMethod_withValidCardInfo() {
            // arrange
            CardType cardType = CardType.SAMSUNG;
            String cardNo = "1111-1111-1111-1111";

            // act
            PaymentMethod paymentMethod = PaymentMethod.of(cardType, cardNo);

            // assert
            assertThat(paymentMethod.getCardType()).isEqualTo(cardType);
            assertThat(paymentMethod.getCardNo()).isEqualTo(cardNo);
        }

        @DisplayName("카드 번호가 null일 경우 NullPointerException이 발생한다.")
        @Test
        void createPaymentMethod_throwsException_whenCardNoIsNull() {
            // arrange
            CardType cardType = CardType.SAMSUNG;

            // act & assert
            assertThatThrownBy(() -> PaymentMethod.of(cardType, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("카드 번호는 필수입니다.");
        }

        @DisplayName("카드 타입이 null일 경우 NullPointerException이 발생한다.")
        @Test
        void createPaymentMethod_throwsException_whenCardTypeIsNull() {
            // arrange
            String cardNo = "1111-1111-1111-1111";

            // act & assert
            assertThatThrownBy(() -> PaymentMethod.of(null, cardNo))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("카드 타입은 필수입니다.");
        }

        @DisplayName("다양한 카드 타입에 대해 정상적으로 생성된다.")
        @Test
        void createPaymentMethod_withDifferentCardTypes() {
            // arrange
            String cardNo = "1111-1111-1111-1111";

            // act & assert
            for (CardType cardType : CardType.values()) {
                PaymentMethod paymentMethod = PaymentMethod.of(cardType, cardNo);
                assertThat(paymentMethod.getCardType()).isEqualTo(cardType);
                assertThat(paymentMethod.getCardNo()).isEqualTo(cardNo);
            }
        }
    }
}