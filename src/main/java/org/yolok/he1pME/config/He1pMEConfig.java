package org.yolok.he1pME.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class He1pMEConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
