package club.escobar.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    public RestClient apifyRestClient(ApifyProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.timeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    @Bean
    public RestClient resendRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    @Bean
    public RestClient zeptoMailRestClient(ZeptoMailProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.apiUrl())
                .build();
    }
}
