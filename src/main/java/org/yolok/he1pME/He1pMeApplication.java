package org.yolok.he1pME;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class He1pMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(He1pMeApplication.class, args);
    }
}
