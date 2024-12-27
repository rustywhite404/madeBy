package com.madeby.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EurekaServerWaiter {

    @Value("${eureka.server.url}")
    private String eurekaServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void waitForEurekaServer() {
        // Eureka 서버가 준비되었는지 확인할 때까지 대기
        while (!isEurekaServerReady()) {
            try {
                System.out.println("Eureka 서버 준비 대기 중...");
                Thread.sleep(5000); // 5초마다 확인
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Eureka 서버 준비 완료!");
    }

    private boolean isEurekaServerReady() {
        try {
            restTemplate.getForObject(eurekaServerUrl + "/eureka/apps", String.class);
            return true;
        } catch (Exception e) {
            // 서버가 준비되지 않았거나 연결에 실패한 경우
            return false;
        }
    }
}