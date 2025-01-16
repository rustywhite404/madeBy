## 📖 목차
1. [🚀 프로젝트 소개](#-프로젝트-소개)
2. [⚙️ 주요 구현 내용](#-주요-구현-내용)
3. [⚡ 성능 최적화 사례](#-성능-최적화-사례)
4. [🐞 트러블 슈팅](#-트러블-슈팅)
5. [📌 기술적 의사결정](#-기술적-의사결정)
6. [📐 System Architecture](#-system-architecture)
7. [⏳ Sequence Diagram](#-sequence-diagram)
8. [📜 산출물](#-산출물)

---
# 선착순 구매 e-commerce : MadeBy

## 🚀 프로젝트 소개

기본적인 전자상거래 뿐만 아니라 한정 상품에 대한 선착순 구매 기회가 있는 E-commerce 사이트입니다.  
쇼핑몰, 중고거래 사이트 등 다양한 형태의 서비스로 변화할 수 있는 발판이 되는 환경을 구현하였습니다.  
**프로젝트 수행 기간** : 2024.12.18 ~ 2025.01.15

**Backend**

![springboot](https://img.shields.io/badge/-springboot-333333?style=flat&logo=springboot)
![springsecurity](https://img.shields.io/badge/-springsecurity-333333?style=flat&logo=springsecurity)
![Redis](https://img.shields.io/badge/-Redis-333333?style=flat&logo=Redis)
![elasticsearch](https://img.shields.io/badge/-elasticsearch-333333?style=flat&logo=elasticsearch)
![Kafka](https://img.shields.io/badge/-Kafka-333333?style=flat&logo=apachekafka)
![Caffeine](https://img.shields.io/badge/-Caffeine-333333?style=flat&logo=CoffeeScript)
![MySQL](https://img.shields.io/badge/-MySQL-333333?style=flat&logo=mysql)
![Java](https://img.shields.io/badge/-Java-333333?style=flat&logo=Java&logoColor=007396)
![JPA](https://img.shields.io/badge/-JPA-333333?style=flat&logo=JPA)

**DevOps & Testing**

![Git](https://img.shields.io/badge/-Git-333333?style=flat&logo=git)
![GitHub](https://img.shields.io/badge/-GitHub-333333?style=flat&logo=github)
![Docker](https://img.shields.io/badge/-Docker-333333?style=flat&logo=docker)
![nGrinder](https://img.shields.io/badge/-nGrinder-333333?style=flat&logo=nGrinder)
![Scouter](https://img.shields.io/badge/-Scouter-333333?style=flat&logo=Scouter)
![K6](https://img.shields.io/badge/-k6-333333?style=flat&logo=k6)
![Postman](https://img.shields.io/badge/-Postman-333333?style=flat&logo=postman)


## 🔨 주요 구현 내용

- **MSA** 기반으로 서비스 독립성과 확장성 향상
- **Eureka** 서비스 디스커버리와 **API Gateway**를 활용한 동적 서비스 등록 및 라우팅 구현
- **OpenFeign**을 통한 외부 모듈 통신, **Resilience4j** Circuit Breaker와 Retry로 회복 탄력성 강화
- **Redis**와 Caffeine을 이용한 캐싱 처리로 서비스 성능 최적화
- **Kafka**를 통한 이벤트 기반 처리로 안정적인 트랜잭션 관리 및 실패 보상(Choreography SAGA)
- **Elastic Search**를 도입하여 검색 결과 정확도 및 속도 향상
- **Docker Compose**로 컨테이너 기반의 통합 개발/배포 환경 구성
- **Naver Open API**를 이용한 상품 데이터베이스 구축, Naver SMTP로 이메일 인증 구현

## ⚡ 성능 최적화 사례
- **주문 결제 API 성능 개선을 위한 시도**
    - 조회가 빈번한 컬럼 복합 인덱스 처리
    - MySQL 스케일 아웃을 통해 각 서버의 max Connection증가 및 장애 독립성 보장
    - 높은 트래픽을 유발하는 한정 상품은 Redis에 정보를 등록해두고 캐시에서 조회, 일반 상품은 Feign Client로 조회하도록 비즈니스 로직 설계 변경
    - GC 사용량을 줄이기 위한 코드 개선
    - 개선 결과 :  
      각 개선점에 대한 상세 내용 및 상세 테스트 결과는 여기에서 확인하실 수 있습니다 → [ 페이지 바로가기 ]          
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
      GC 사용량 감소 → 애플리케이션 성능 저하 방지 

      Heap 사용량 증가 → 많은 객체를 메모리에 유지하여 객체 재생성 비용 절약
---
- **상품 검색 성능 개선**

  60,000건의 실제 커머스 상품 데이터 기반 상품명 검색 정확도 및 성능 향상
    - Offset → Cursor 기반 페이징 처리
    - **Elastic Search**의 한글 검색 엔진 `nori`를 활용, 검색 정확도 개선 -> [ 테스트 결과 보기 ]
    - 검색 엔진에 문제가 생겼을 경우 Caffeine 캐시를 통해 빠른 응답 제공          
      ⇒ TTL을 설정하여 데이터 신선도를 보장하고, **Window TinyLFU** 적용으로 인기 데이터 유지

    - 개선 결과 :  
      각 개선점에 대한 상세 내용 및 상세 테스트 결과는 여기에서 확인하실 수 있습니다 → [ 페이지 바로가기 ]  
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
      각 개선점에 대한 상세 내용 및 상세 테스트 결과는 여기에서 확인하실 수 있습니다 → [ 페이지 바로가기 ]  
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
- 동시성 제어를 위한 시도들(Redis Lock, Lua Script)
- 주문 결제 성능 개선 중 생긴 Redis 역직렬화 문제 해결
- 검색 수행 시 N+1 문제 해결
- Circuit Breaker 구현 위치와 Exception 필터링 문제 해결
- Jackson 직렬화-역직렬화 과정에서 발생한 순환 참조 문제 해결
- AccessToken이 발급 되었는데 사용자가 존재하지 않는 문제 해결
- AccessToken 재발급 과정에서 루프 발생
- nGrinder와 K6로 테스트 중 생긴 문제 해결

## 📌 기술적 의사결정

- 선착순 구매 시스템 설계 시 재고 수량 표기 방법
- 예상 가능한 유저 불편목록과 중요도 결정
- 모듈 간 통신 시 Feign Client vs. Kafka 기술 스택 결정
- Elastic Search 형태소 검색 & 비정형 텍스트 검색 방식 결정
- 내부 캐시 ehCache vs. Caffeine 기술 스택 결정




## 📐 System Architecture
![Structure](https://github.com/user-attachments/assets/5852e8eb-e9c4-4117-807b-1d6fac444f2a)

## ⏳ Sequence Diagram
![주문 결제 시퀀스 다이어그램](https://github.com/user-attachments/assets/1bdd3354-2e58-4358-9a95-3de544e0f0cc)

## 📜 산출물
<a href="https://documenter.getpostman.com/view/22818248/2sAYJ3FhBW#intro" target="_blank">📂 API 명세서</a>  
<a href="https://github.com/rustywhite404/madeBy/wiki/ERD" target="_blank">📂 ERD</a>  
<a href="https://github.com/rustywhite404/madeBy/wiki/%ED%8F%B4%EB%8D%94-%EA%B5%AC%EC%A1%B0%EB%8F%84" target="_blank">📂 폴더 구조도</a>  
<a href="https://github.com/rustywhite404/madeBy/wiki/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%ED%99%98%EA%B2%BD-%EC%84%A4%EC%A0%95-%EB%B0%8F-%EC%8B%A4%ED%96%89-%EA%B0%80%EC%9D%B4%EB%93%9C" target="_blank">📂 프로젝트 환경 설정 및 실행 가이드</a>



