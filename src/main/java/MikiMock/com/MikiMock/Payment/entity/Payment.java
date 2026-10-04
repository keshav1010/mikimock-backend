package MikiMock.com.MikiMock.Payment.entity;


import MikiMock.com.MikiMock.User.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {

                @Index(
                        name = "idx_payment_reference",
                        columnList = "payment_reference"
                ),

                @Index(
                        name = "idx_payment_provider_order",
                        columnList = "provider_order_id"
                ),

                @Index(
                        name = "idx_payment_user_status",
                        columnList = "user_id,status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /*
     * Our internal payment identifier.
     *
     * Example:
     * PAY_A8D91F...
     */
    @Column(
            name = "payment_reference",
            nullable = false,
            unique = true,
            updatable = false,
            length = 100
    )
    private String paymentReference;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "subscription_plan_id",
            nullable = false
    )
    private SubscriptionPlan subscriptionPlan;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PaymentProviderType provider;

    /*
     * Razorpay:
     * order_xxx
     *
     * PayPal:
     * PayPal order ID
     */
    @Column(
            name = "provider_order_id",
            unique = true,
            length = 255
    )
    private String providerOrderId;

    /*
     * Actual successful transaction.
     *
     * Razorpay:
     * pay_xxx
     */
    @Column(
            name = "provider_payment_id",
            unique = true,
            length = 255
    )
    private String providerPaymentId;

    /*
     * For providers such as Razorpay.
     */
    @Column(
            name = "provider_signature",
            length = 1000
    )
    private String providerSignature;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PaymentStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;

        this.updatedAt = now;

        if (this.status == null) {

            this.status =
                    PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {

        this.updatedAt =
                LocalDateTime.now();
    }
}


//@Entity
//@Table(name = "payments")
//
//@Getter
//@Setter
//@Builder
//
//@NoArgsConstructor
//@AllArgsConstructor
//public class Payment {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(
//            name = "payment_id",
//            nullable = false,
//            unique = true
//    )
//    private UUID paymentId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private User user;
//
//    @Column(nullable = false)
//    private Double amount;
//
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus status;
//
//    @Column(name = "transaction_id")
//    private String transactionId;
//
//    @Column(name = "order_id")
//    private String orderId;
//
//    @Column(name = "payment_reference")
//    private String paymentReference;
//
//    @Column(name = "signature")
//    private String signature;
//
//    @Column(name = "currency")
//    private String currency;
//
//    @Column(name = "payment_method")
//    private String paymentMethod;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    public void prePersist() {
//
//        this.paymentId = UUID.randomUUID();
//
//        this.createdAt = LocalDateTime.now();
//
//        this.updatedAt = LocalDateTime.now();
//
//        if (this.status == null) {
//
//            this.status = PaymentStatus.PENDING;
//        }
//
//        if (this.currency == null) {
//
//            this.currency = "INR";
//        }
//    }
//
//    @PreUpdate
//    public void preUpdate() {
//
//        this.updatedAt = LocalDateTime.now();
//    }
//
//}