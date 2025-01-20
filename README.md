## 📖 목차
1. [🚀 프로젝트 소개](#-프로젝트-소개)
2. [📐 System Architecture](#-system-architecture) 
3. [🗂️ ERD](#-erd) 
4. [⏳ Sequence Diagram](#-sequence-diagram)
5. [🔨 주요 구현 내용](#-주요-구현-내용)
6. [⚡ 성능 최적화 사례](#-성능-최적화-사례)
7. [🐞 트러블 슈팅](#-트러블-슈팅)
8. [📌 기술적 의사결정](#-기술적-의사결정)

---
# 선착순 구매 e-commerce : MadeBy

## 🚀 프로젝트 소개

기본적인 전자상거래 뿐만 아니라 한정 상품에 대한 선착순 구매 기회가 있는 E-commerce 사이트입니다.  
쇼핑몰, 중고거래 사이트 등 다양한 형태의 서비스로 변화할 수 있는 발판이 되는 환경을 구현하였습니다.  
- **프로젝트 수행 기간** : 2024.12.18 ~ 2025.01.15

- **Backend**  

    ![Java](https://img.shields.io/badge/-Java21-333333?style=flat&logo=Java&logoColor=007396)
    ![springboot](https://img.shields.io/badge/-springboot3.4-333333?style=flat&logo=springboot)
    ![springdataJPA](https://img.shields.io/badge/-springDataJPA-333333?style=flat&logo=spring)
    ![springCloud](https://img.shields.io/badge/-springCloud-333333?style=flat&logo=spring)
    ![springsecurity](https://img.shields.io/badge/-springsecurity-333333?style=flat&logo=springsecurity)
    ![JWT](https://img.shields.io/badge/-JWT-333333?style=flat&logo=JSON%20web%20tokens)

- **Data Systems**  

    ![Redis](https://img.shields.io/badge/-Redis7.0-333333?style=flat&logo=Redis)
    ![elasticsearch](https://img.shields.io/badge/-elasticsearch-333333?style=flat&logo=elasticsearch)
    ![Kafka](https://img.shields.io/badge/-Kafka-333333?style=flat&logo=apachekafka)
    ![Caffeine](https://img.shields.io/badge/-Caffeine-333333?style=flat&logo=CoffeeScript)
    ![MySQL](https://img.shields.io/badge/-MySQL-333333?style=flat&logo=mysql)

- **DevOps & Testing**

    ![Docker](https://img.shields.io/badge/-Docker-333333?style=flat&logo=docker)
    ![nGrinder](https://img.shields.io/badge/-nGrinder-333333?style=flat&logo=nGrinder)
    ![Scouter](https://img.shields.io/badge/-Scouter-333333?style=flat&logo=Scouter)
    ![K6](https://img.shields.io/badge/-k6-333333?style=flat&logo=k6) 

- **산출물**  
  [📂 API 명세서](https://documenter.getpostman.com/view/22818248/2sAYJ3FhBW#intro) [📂 프로젝트 환경 설정 및 실행 가이드](https://github.com/rustywhite404/madeBy/wiki/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%ED%99%98%EA%B2%BD-%EC%84%A4%EC%A0%95-%EB%B0%8F-%EC%8B%A4%ED%96%89-%EA%B0%80%EC%9D%B4%EB%93%9C)
    


## 📐 System Architecture
![Structure](https://github.com/user-attachments/assets/5852e8eb-e9c4-4117-807b-1d6fac444f2a)   

## 🗂️ ERD
![ERD](https://i.imgur.com/BgiP7ht.png)

## ⏳ Sequence Diagram
![주문 결제 시퀀스 다이어그램](https://github.com/user-attachments/assets/1bdd3354-2e58-4358-9a95-3de544e0f0cc)


## 🔨 주요 구현 내용

- **MSA(MicroService Architecture) 적용** :  
  모놀리식 서비스를 MSA로 리팩토링 하여 서비스 독립성과 확장성 향상
- **Eureka 서비스 디스커버리와 API Gateway 사용** :  
  각 서비스 모듈 관리를 위하여 동적 서비스 등록 및 라우팅 구현
- **Elastic Search 도입** :  
  한글 형태소 분석 엔진을 사용하여 검색 결과 정확도 및 속도 향상
- **Redis와 Caffeine을 이용한 캐싱 처리** :  
  Remote Cache와 Local Cache를 적절하게 활용, 성능과 비용을 고려한 자원 사용
- **Redis와 Lua Script를 이용한 동시성 처리** :  
  원자적 재고 감소 설계로 다량의 트래픽 환경에서도 데이터 정합성 유지
- **Kafka를 통한 이벤트 기반 처리** :  
  비동기식 이벤트를 발행하여 결제에 따른 주문 작성 및 Choreography SAGA패턴을 이용한 실패 보상
- **Kafka, OpenFeign을 통한 외부 모듈 통신**
- **Resilience4j Circuit Breaker와 Retry로 회복 탄력성 구현**
- **Docker Compose로 컨테이너 기반의 통합 개발/배포 환경 구성**

## ⚡ 성능 최적화 사례
- **주문 결제 API 성능 개선**
    - 조회가 빈번한 컬럼 복합 인덱스 처리
    - MySQL 스케일 아웃을 통해 각 서버의 max Connection증가 및 장애 독립성 보장
    - 높은 트래픽을 유발하는 한정 상품은 Redis에서 조회, 일반 상품은 Feign Client로 조회하도록 비즈니스 로직 설계 변경
    - GC 사용량을 줄이기 위한 코드 개선
    - 개선 결과 :  
      각 개선점에 대한 상세 내용 및 상세 테스트 결과 확인 → [ 🔗 페이지 바로가기 ](https://github.com/rustywhite404/madeBy/wiki/%EC%A3%BC%EC%9A%94-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%82%B4%EC%97%AD-&-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A0%84%ED%9B%84-%EB%B9%84%EA%B5%90#1-%EC%A3%BC%EB%AC%B8-%EA%B2%B0%EC%A0%9C-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%82%B4%EC%97%AD-%EB%B0%8F-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A0%84%ED%9B%84-%EB%B9%84%EA%B5%90)

      **TPS** : 평균 62% 개선
      <table>
      <tr>
      <th></th>
      <th>Vuser 200명</th>
      <th>Vuser 400명</th>
        </tr>
        <tr>
            <td>개선 전</td>
            <td>416.6</td>
            <td>367.4</td>
        </tr>
        <tr class="highlight">
            <td>개선 후</td>
            <td>630.6</td>
            <td>636.0</td>
        </tr>
      </table>

      **Mean Test Time(Latency)** : 지연시간 평균 31% 감소
        <table>
        <tr>
            <th></th>
            <th>Vuser 200명</th>
            <th>Vuser 300명</th>
        </tr>
        <tr>
            <td>개선 전</td>
            <td>445.50ms</td>
            <td>769.66ms</td>
        </tr>
        <tr class="highlight">
            <td>개선 </td>
            <td>294.63ms</td>
            <td>550.30ms</td>
        </tr>
      </table>  
      
      **GC Count** :          
      (개선 전) 최대 4~5회 → (개선 후) 평균 2회 미만         
      → GC 사용 횟수를 줄여 애플리케이션 성능 저하 방지    
      
      **Heap 사용량(MB)** :  
      (개선 전) 150MB,변동이 크고 불규칙 → (개선 후) 200MB 유지, 일정한 패턴    
      → 많은 객체를 메모리에 유지하여 객체 재생성 비용 절약   
---
- **상품 검색 성능 개선**

  60,000건의 실제 커머스 상품 데이터 기반 상품명 검색 정확도 및 성능 향상
    - Offset → Cursor 기반 페이징 처리
    - **Elastic Search**의 한글 검색 엔진 `nori`를 활용, 검색 정확도 개선  → [ 🔗 페이지 바로가기 ](https://github.com/rustywhite404/madeBy/wiki/Elastic-search%EC%97%90%EC%84%9C-%ED%95%9C%EA%B8%80-%ED%98%95%ED%83%9C%EC%86%8C-%EB%8B%A8%EC%9C%84-%EA%B2%80%EC%83%89-%EC%84%A4%EC%A0%95)
    - 검색 엔진에 문제가 생겼을 경우 Caffeine 캐시를 통해 빠른 응답 제공          
      ⇒ TTL을 설정하여 데이터 신선도를 보장하고, **Window TinyLFU** 적용으로 인기 데이터 유지

    - 개선 결과 :  
      각 개선점에 대한 상세 내용 및 상세 테스트 결과 확인 → [ 🔗 페이지 바로가기 ](https://github.com/rustywhite404/madeBy/wiki/%EC%A3%BC%EC%9A%94-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%82%B4%EC%97%AD-&-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A0%84%ED%9B%84-%EB%B9%84%EA%B5%90#2-%EC%83%81%ED%92%88-%EA%B2%80%EC%83%89-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%82%B4%EC%97%AD-%EB%B0%8F-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A0%84%ED%9B%84-%EB%B9%84%EA%B5%90)   
      **TPS** : Elastic Search 적용 후 TPS 평균 23,504% 증가
      <table>
      <tr>
      <th></th>
      <th>Vuser 100명</th>
      <th>Vuser 200명</th>
      <th>Vuser 300명</th>
        </tr>
        <tr>
            <td>개선 전</td>
            <td>17.2</td>
            <td>16.9</td>
            <td>13.4</td>
        </tr>
        <tr>
            <td>Caffeine 적용</td>
            <td>417.5</td>
            <td>303.6</td>
            <td>288.1</td>
        </tr>
        <tr class="highlight">
            <td>Elastic Search 적용</td>
            <td>4,049.3</td>
            <td>3,656.6</td>
            <td>3,434.9</td>
        </tr>
      </table>  

      **Mean Test Time(Latency)** : Elastic Search 적용 후 지연시간 평균 99.07% 감소
        <table>
        <tr>
            <th></th>
            <th>Vuser 100명</th>
            <th>Vuser 200명</th>
            <th>Vuser 300명</th>
        </tr>
        <tr>
            <td>개선 전</td>
            <td>1,752.82ms</td>
            <td>4,540.22ms</td>
            <td>10,216.26ms</td>
        </tr>
        <tr>
                <td>Caffeine 적용</td>
                <td>229.69ms</td>
                <td>573.00ms</td>
                <td>897.28ms</td>
            </tr>
        <tr class="highlight">
            <td>Elastic Search 적용</td>
            <td>21.55ms</td>
            <td>41.83ms</td>
            <td>66.22ms</td>
        </tr>
      </table>  
---
- **상품 조회 성능 개선**

  60,000건의 실제 커머스 상품 데이터 기반 상품 조회 시 성능 개선.     
  사용자가 상품 목록의 1~15페이지를 랜덤으로 조회한다고 가정, nGrinder 부하 테스트 진행.

    - 복합 인덱스 처리
    - N+1 문제 해결을 위한 Dto 프로젝션 처리
    - Caffeine 캐시를 통한 상품 목록, 검색 결과 캐싱 처리          
      ⇒ 모든 상품을 캐시에 저장하지 않고, 사용자가 자주 확인하는 1~10페이지의 데이터만 캐싱 처리하여 서버 자원을 효율적으로 사용

    - 개선 결과 :  
      각 개선점에 대한 상세 내용 및 상세 테스트 결과 확인 → [ 🔗 페이지 바로가기 ](https://github.com/rustywhite404/madeBy/wiki/%EC%A3%BC%EC%9A%94-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%82%B4%EC%97%AD-&-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A0%84%ED%9B%84-%EB%B9%84%EA%B5%90#3-%EC%83%81%ED%92%88-%EC%A1%B0%ED%9A%8C-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%82%B4%EC%97%AD-%EB%B0%8F-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A0%84%ED%9B%84-%EB%B9%84%EA%B5%90)    
      **TPS** : 약 `300%` 증가
      <table>
      <tr>
      <th></th>
      <th>Vuser 200명</th>
      <th>Vuser 400명</th>
      <th>Vuser 1,000명</th>
        </tr>
        <tr>
            <td>개선 전</td>
            <td>843.2</td>
            <td>744.8</td>
            <td>534.3</td>
        </tr>
        <tr class="highlight">
            <td>개선 후</td>
            <td>2,651.0</td>
            <td>2,435.0</td>
            <td>1,876.0</td>
        </tr>
      </table>  

      **Mean Test Time(Latency)** : 약 `80%` 감소
        <table>
        <tr>
            <th></th>
            <th>Vuser 200명</th>
            <th>Vuser 400명</th>
            <th>Vuser 1,000명</th>
        </tr>
        <tr>
            <td>개선 전</td>
            <td>224.39ms</td>
            <td>492.37ms</td>
            <td>1,499.98ms</td>
        </tr>
        <tr class="highlight">
            <td>개선 후</td>
            <td>64.15ms</td>
            <td>92.95ms</td>
            <td>383.36ms</td>
        </tr>
      </table>  



## 🐞 트러블 슈팅
- [동시성 제어를 위한 시도들(Redis Lock, Lua Script)](https://github.com/rustywhite404/madeBy/wiki/%EB%8F%99%EC%8B%9C%EC%84%B1-%EC%A0%9C%EC%96%B4%EB%A5%BC-%EC%9C%84%ED%95%9C-%EC%8B%9C%EB%8F%84)  
  RLock만으로는 원자적 동시성 제어가 되지 않는 이유를 알아보고, Lua Script를 이용하여 원자적 재고 감소 처리에 성공했습니다.    
- [주문 결제 성능 개선 중 생긴 Redis 역직렬화 문제 해결](https://github.com/rustywhite404/madeBy/wiki/Redis-%EC%97%AD%EC%A7%81%EB%A0%AC%ED%99%94-%EB%AC%B8%EC%A0%9C)  
  Redis를 사용한다고 무조건 성능이 개선 되는 게 아니라, 처리 비용을 고려한 코드가 성능에 더 큰 영향을 미칠 수 있다는 점을 실감했습니다.  
- [Circuit Breaker 구현 위치와 Exception 필터링 문제 해결](https://github.com/rustywhite404/madeBy/wiki/Circuit-Breaker,-Exception-%ED%95%84%ED%84%B0%EB%A7%81-%EB%AC%B8%EC%A0%9C)  
  잘못된 설계는 부수적인 트러블까지 생산하기도 하므로, 적절한 초기 설계의 중요성을 알게 되었습니다.   
- [검색 수행 시 N+1 문제 해결](https://github.com/rustywhite404/madeBy/wiki/JPA-N+1-%EB%AC%B8%EC%A0%9C)  
  JPA에서 흔히 발생하는 N+1 문제를 해결하는 과정에서 유지보수성과 코드 명확성에 대해 고민하였습니다.  
- [Jackson 직렬화-역직렬화 과정에서 발생한 순환 참조 문제 해결](https://github.com/rustywhite404/madeBy/wiki/Jackson-%EC%A7%81%EB%A0%AC%ED%99%94,-%EC%97%AD%EC%A7%81%EB%A0%AC%ED%99%94-%EC%88%9C%ED%99%98-%EC%B0%B8%EC%A1%B0-%EB%AC%B8%EC%A0%9C)  
  ORM 사용 시 주의해야 할 설계 이슈를 겪으며 적절한 엔티티 연관관계의 중요성을 깨달았습니다.   
- [AccessToken이 발급 되었는데 사용자가 존재하지 않는 문제 해결](https://github.com/rustywhite404/madeBy/wiki/AccessToken-%EA%B4%80%EB%A0%A8-%EB%AC%B8%EC%A0%9C#1-accesstoken%EC%9D%B4-%EB%B0%9C%EA%B8%89-%EB%90%98%EC%97%88%EB%8A%94%EB%8D%B0%EB%8F%84-%EC%82%AC%EC%9A%A9%EC%9E%90%EB%A5%BC-%EC%B0%BE%EC%9D%84-%EC%88%98-%EC%97%86%EB%8A%94-%EB%AC%B8%EC%A0%9C)
- [AccessToken 재발급 과정에서 루프 발생](https://github.com/rustywhite404/madeBy/wiki/AccessToken-%EA%B4%80%EB%A0%A8-%EB%AC%B8%EC%A0%9C#2-accesstoken-%EC%9E%AC%EB%B0%9C%EA%B8%89-%EB%8B%A8%EA%B3%84%EC%97%90%EC%84%9C-%EB%A3%A8%ED%94%84-%EB%B0%9C%EC%83%9D)  
  Spring Security의 처리 흐름과 각 모듈이 가져야 할 책임에 대해 고민하게 된 문제였습니다.   
- [nGrinder를 Docker에서 실행했을 때의 성능 저하 문제](https://github.com/rustywhite404/madeBy/wiki/nGrinder-%ED%85%8C%EC%8A%A4%ED%8A%B8-%ED%99%98%EA%B2%BD-%EB%AC%B8%EC%A0%9C)  
  테스트 툴들의 차이에 대해 이해하고 실행 환경의 중요성을 경험하였습니다.  

## 📌 기술적 의사결정
- [선착순 구매 시스템의 재고 수량 표기 방법 결정하기](https://github.com/rustywhite404/madeBy/wiki/%EC%84%A0%EC%B0%A9%EC%88%9C-%EA%B5%AC%EB%A7%A4-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EC%9E%AC%EA%B3%A0-%ED%91%9C%EA%B8%B0-%EC%84%A4%EA%B3%84)
- [사용자 경험을 고려한 기능 중요도 결정 : '줬다 뺏느니 안 주는 게 낫다'](https://github.com/rustywhite404/madeBy/wiki/%EC%98%88%EC%83%81-%EA%B0%80%EB%8A%A5%ED%95%9C-%EC%9C%A0%EC%A0%80-%EB%B6%88%ED%8E%B8%EB%AA%A9%EB%A1%9D%EA%B3%BC-%EC%A4%91%EC%9A%94%EB%8F%84-%EA%B2%B0%EC%A0%95)
- [Feign Client vs. Kafka : 모듈 간 통신, 무조건 비동기 방식이 좋은 걸까?](https://github.com/rustywhite404/madeBy/wiki/%EB%AA%A8%EB%93%88-%EA%B0%84-%ED%86%B5%EC%8B%A0-%EC%8B%9C-Feign-Client-vs.-Kafka)
- [Elastic Search 형태소 검색 & 비정형 텍스트 검색 방식 결정](https://github.com/rustywhite404/madeBy/wiki/Elastic-search-%ED%95%9C%EA%B8%80-%ED%98%95%ED%83%9C%EC%86%8C-%EB%8B%A8%EC%9C%84-%EA%B2%80%EC%83%89-%EC%84%A4%EC%A0%95)
- [내부 캐시 ehCache vs. Caffeine : 페이지 교체 알고리즘을 고려한 선택](https://github.com/rustywhite404/madeBy/wiki/%EB%82%B4%EB%B6%80-%EC%BA%90%EC%8B%9C-ehCache-vs.-Caffeine)

