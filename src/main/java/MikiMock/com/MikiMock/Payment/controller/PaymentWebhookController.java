package MikiMock.com.MikiMock.Payment.controller;

import MikiMock.com.MikiMock.Payment.entity.PaymentProviderType;
import MikiMock.com.MikiMock.Payment.service.PaymentWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/webhook")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;


    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(

            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            )
            String signature,

            @RequestBody
            String rawPayload
    ) {

        log.info(
                "Razorpay webhook received"
        );


        if (
                signature == null ||
                        signature.isBlank()
        ) {

            log.warn(
                    "Razorpay webhook rejected | signature missing"
            );

            return ResponseEntity
                    .status(
                            HttpStatus.BAD_REQUEST
                    )
                    .build();
        }


        paymentWebhookService.processWebhook(
                PaymentProviderType.RAZORPAY,
                signature,
                rawPayload
        );


        /*
         * Razorpay only needs confirmation that
         * the webhook was accepted.
         */
        return ResponseEntity
                .ok()
                .build();
    }
}