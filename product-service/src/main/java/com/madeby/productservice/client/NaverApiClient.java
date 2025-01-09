package com.madeby.productservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class NaverApiClient {

    private final WebClient webClient;

    private static final String NAVER_API_PATH = "/v1/search/shop.json";

    public String searchProducts(String query, int start, int display) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(NAVER_API_PATH) // URL 경로
                            .queryParam("query", query)
                            .queryParam("start", 1)
                            .queryParam("display", display)
                            .build())
                    .header("X-Naver-Client-Id", "[My naver Client Id]") // 네이버 애플리케이션의 Client ID
                    .header("X-Naver-Client-Secret", "[My Client Secret]") // 네이버 애플리케이션의 Client Secret
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (
                WebClientResponseException e) {
            // 요청 실패 시 로그 출력
            System.err.println("네이버 API 요청 실패");
            System.err.println("상태 코드: " + e.getStatusCode());
            System.err.println("응답 바디: " + e.getResponseBodyAsString());
            throw e;
        }
    }


}
