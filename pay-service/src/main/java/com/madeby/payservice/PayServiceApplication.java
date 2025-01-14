package com.madeby.payservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@ComponentScan(basePackages = {"com.madeBy.shared", "com.madeby.payservice"})
public class PayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }

}
