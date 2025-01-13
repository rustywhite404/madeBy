package com.madeby.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients
@ComponentScan(basePackages = {"com.madeBy.shared", "com.madeby.productservice"})
@EnableJpaRepositories(basePackages = "com.madeby.productservice.repository") // JPA 저장소 경로
@EnableElasticsearchRepositories(basePackages = "com.madeby.productservice.elasticsearch") // Elasticsearch 저장소 경로
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}
