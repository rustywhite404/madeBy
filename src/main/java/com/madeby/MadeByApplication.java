package com.madeby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@EnableJpaAuditing //Test 코드 실행 시 방해가 되어서 JpaConfig로 따로 뺌
@SpringBootApplication
public class MadeByApplication {

    public static void main(String[] args) {
        SpringApplication.run(MadeByApplication.class, args);
    }

}
