package MikiMock.com.MikiMock.Payment.repository;

import MikiMock.com.MikiMock.Payment.entity.SubscriptionPlan;
import MikiMock.com.MikiMock.Payment.entity.SubscriptionPlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByPlanCodeAndActiveTrue(SubscriptionPlanCode planCode);
}
