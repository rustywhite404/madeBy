package com.madeby.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableJpaAuditing //Test 코드 실행 시 방해가 되어서 JpaConfig로 따로 뺌
@SpringBootApplication
@EnableScheduling
//@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {"com.madeBy.shared", "com.madeby.orderservice"})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
