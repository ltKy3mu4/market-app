package org.yandex.mymarketapp.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.yandex.payment.invoker.ApiClient;
import org.yandex.payment.api.BalanceApi;
import org.yandex.payment.api.PaymentsApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class PaymentApiWebClientConfig {

    @Value("${payment.service.url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient paymentApiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(paymentServiceUrl);

        return apiClient;
    }

    @Bean
    public BalanceApi balanceApi(ApiClient paymentApiClient) {
        return new BalanceApi(paymentApiClient);
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new PaymentsApi(paymentApiClient);
    }

    @PostConstruct
    public void init(){
        log.info("Payment service URL set to: {}", paymentServiceUrl);
    }
}
