package MikiMock.com.MikiMock.Payment.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderRequest {

    @NonNull
    private Double amount;

    @NonNull
    private Integer monthPlan;

    private String currency;

    private String receipt;

}
