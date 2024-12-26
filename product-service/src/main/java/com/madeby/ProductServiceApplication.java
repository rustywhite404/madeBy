package com.madeby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableJpaAuditing //Test 코드 실행 시 방해가 되어서 JpaConfig로 따로 뺌
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.madeBy.shared.config")
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}
