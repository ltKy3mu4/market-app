package org.yandex.mymarketapp.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.yandex.payment.invoker.ApiClient;

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
    public ApiClient paymentApiClient(ReactiveOAuth2AuthorizedClientManager authManager) {
        WebClient webClient = WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .filter((request, next) -> authManager.authorize(OAuth2AuthorizeRequest
                                .withClientRegistrationId("shop-client")
                                .principal("system")
                                .build())
                        .map(OAuth2AuthorizedClient::getAccessToken)
                        .map(token -> ClientRequest.from(request)
                                .headers(h -> h.setBearerAuth(token.getTokenValue()))
                                .build())
                        .flatMap(next::exchange))
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(paymentServiceUrl);

        return apiClient;
    }

    @Bean
    public org.openapitools.client.api.BalanceApi balanceApi(ApiClient paymentApiClient) {
        return new org.openapitools.client.api.BalanceApi(paymentApiClient);
    }

    @Bean
    public org.openapitools.client.api.PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new org.openapitools.client.api.PaymentsApi(paymentApiClient);
    }

    @PostConstruct
    public void init(){
        log.info("Payment service URL set to: {}", paymentServiceUrl);
    }
}
