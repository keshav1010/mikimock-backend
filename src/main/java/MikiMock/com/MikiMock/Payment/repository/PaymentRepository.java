package MikiMock.com.MikiMock.Payment.repository;

import MikiMock.com.MikiMock.Payment.entity.Payment;
import MikiMock.com.MikiMock.Payment.entity.PaymentStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment>
    findByPaymentReference(
            String paymentReference
    );

    Optional<Payment>
    findByProviderOrderId(
            String providerOrderId
    );

    Optional<Payment>
    findByProviderPaymentId(
            String providerPaymentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.providerOrderId = :providerOrderId
        """)
    Optional<Payment> findByProviderOrderIdForUpdate(
            @Param("providerOrderId")
            String providerOrderId
    );

    Optional<Payment> findById(Long id);

    @Lock(
            LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.id = :Id
            """)
    Optional<Payment> findByIdForUpdate(
            @Param("Id")
            Long Id
    );

    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.status = :status
        AND p.createdAt <= :before
        ORDER BY p.createdAt ASC
        """)
    List<Payment> findPaymentsForReconciliation(
            @Param("status")
            PaymentStatus status,

            @Param("before")
            LocalDateTime before,

            Pageable pageable
    );


//    Optional<Payment> findByPublicId(
//            String publicId
//    );
//
//
//    Optional<Payment> findByGatewayOrderId(
//            String gatewayOrderId
//    );
//
//
//    Optional<Payment> findByGatewayPaymentId(
//            String gatewayPaymentId
//    );
//
//
//    Optional<Payment> findByIdempotencyKey(
//            String idempotencyKey
//    );
//
//
//    List<Payment> findByUserIdOrderByCreatedAtDesc(
//            Long userId
//    );
//
//
//    List<Payment> findByStatus(
//            PaymentStatus status
//    );

}
