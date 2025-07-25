
# 🏷 브랜드 & 상품 (Brands / Products)

## 1. 상품 목록 조회
```mermaid
sequenceDiagram
    actor U as User
    participant PA as ProductApi
    participant PS as ProductService
    participant PR as ProductRepository

    U->>+PA: 상품 목록 조회 요청 (?brandId, ?sort, ?page, ?size)
    PA->>+PS: 상품 목록 조회 요청(조건 포함)
    PS->>+PR: 상품 목록 조회(판매중인 상품, 브랜드)
    alt DB 연결 실패
        PR-->>PA: 500 Internal Server Error
        PA-->>U: 500 Internal Server Error + "상품 조회 중 오류가 발생했습니다."
    end
    PR-->>-PS: 판매중인 상품 목록
    PS-->>-PA: 판매중인 상품 목록(정렬)
    PA-->>-U: 200 OK (상품 목록 JSON)
```    

## 2. 상품 정보 조회
```mermaid
sequenceDiagram
    actor U as User
    participant PA as ProductApi
    participant PS as ProductService
    participant PR as ProductRepository

    U->>+PA: 상품 정보 조회 요청 (productId)
    PA->>+PS: 상품 정보 조회 요청(productId)
    PS->>+PR: 상품 정보 조회(productId)
    alt 상품 미존재
        PR-->>U: 404 NOT FOUND + "상품을 찾을 수 없습니다."
    end
    PR-->>-PS: 상품 정보
    PS-->>-PA: 상품 정보
    PA-->>-U: 200 OK (상품 정보 JSON)
```    

## 3. 브랜드 정보 조회
```mermaid
sequenceDiagram
    actor U as User
    participant PA as ProductApi
    participant PS as ProductService
    participant PR as ProductRepository

    U->>+PA: 브랜드 정보 조회 요청 (brandId)
    PA->>+PS: 브랜드 정보 조회 요청(brandId)
    PS->>+PR: 브랜드 정보 조회(brandId)
    alt 브랜드 미존재
        PR-->>U: 404 NOT FOUND + "브랜드를 찾을 수 없습니다."
    end
    PR-->>-PS: 브랜드 정보
    PS-->>-PA: 브랜드 정보
    PA-->>-U: 200 OK (브랜드 정보 JSON)
```    

# ❤️ 좋아요 (Likes)

## 4. 상품 좋아요 등록
```mermaid
sequenceDiagram
    actor U as User
    participant LA as LikesApi
    participant LF as LikesFacade
    participant PS as ProductService
    participant LS as LikesService
    participant LR as LikesRepository
    
    U->>+LA: 상품 좋아요 등록 요청 (X-USER-ID 헤더, productId)
    LA->>+LF: 상품 좋아요 등록 요청 (X-USER-ID 헤더, productId)
    alt 미로그인, 헤더 정보 없음
        LF-->>-LA: 401 UNAUTHORIZED
    end
    LF->>+PS: 상품 정보 조회 (productId)
    alt 상품 미존재
        PS-->>-LA: 404 NOT FOUND
    end
    PS-->>+LF: 상품 정보 (productId)
    LF->>+LS: 상품 좋아요 등록 여부 확인 (userId, productId)
    LS->>+LR: 상품 좋아요 등록 여부 확인
    alt 이미 좋아요 등록됨
        LR-->LA: 400 BAD REQUEST + "이미 좋아요가 등록되어 있습니다."
    end
    LR-->>-LS: 좋아요 등록 가능
    LS->>+LR: 상품 좋아요 등록 (userId, productId)
    LR-->>-LS: 좋아요 등록 성공
    LS-->>-LF: 좋아요 등록 성공
    LF-->>-LA: 201 CREATED + "좋아요가 등록되었습니다."
    LA-->>-U: 201 CREATED + "좋아요가 등록되었습니다."
```    

## 5. 상품 좋아요 취소
```mermaid
sequenceDiagram
    actor U as User
    participant LA as LikesApi
    participant LF as LikesFacade
    participant PS as ProductService
    participant LS as LikesService
    participant LR as LikesRepository
    
    U->>+LA: 상품 좋아요 취소 요청 (X-USER-ID 헤더, productId)
    LA->>+LF: 상품 좋아요 취소 요청 (X-USER-ID 헤더, productId)
    alt 미로그인, 헤더 정보 없음
        LF-->>-LA: 401 UNAUTHORIZED
    end
    LF->>+PS: 상품 정보 조회 (productId)
    alt 상품 미존재
        PS-->>-LA: 404 NOT FOUND
    end
    PS-->>+LF: 상품 정보 (productId)
    LF->>+LS: 상품 좋아요 등록 여부 확인 (userId, productId)
    LS->>+LR: 상품 좋아요 등록 여부 확인
    alt 좋아요 등록되지 않음
        LR-->LA: 400 BAD REQUEST + "좋아요가 등록되어 있지 않습니다."
    end
    LR-->>-LS: 좋아요 등록됨
    LS->>+LR: 상품 좋아요 취소 (userId, productId)
    LR-->>-LS: 좋아요 취소 성공
    LS-->>-LF: 좋아요 취소 성공
    LF-->>-LA: 200 OK + "좋아요가 취소되었습니다."
    LA-->>-U: 200 OK + "좋아요가 취소되었습니다."
```    

## 6. 내가 좋아요 한 상품 목록 조회
```mermaid
sequenceDiagram
    actor U as User
    participant LA as LikesApi
    participant LF as LikesFacade
    participant PS as ProductService
    participant LS as LikesService
    participant LR as LikesRepository
    
    U->>+LA: 내가 좋아요 한 상품 목록 조회 요청 (X-USER-ID 헤더)
    LA->>+LF: 내가 좋아요 한 상품 목록 조회 요청 (X-USER-ID 헤더)
    alt 미로그인, 헤더 정보 없음
        LF-->>LA: 401 UNAUTHORIZED
    end
    LF->>+LS: 내가 좋아요 한 상품 목록 조회 (userId)
    LS->>+LR: 내가 좋아요 한 상품 목록 조회 (userId)
    alt 좋아요 목록 없음
        LR-->>-LA: 404 NOT FOUND + "좋아요 목록이 없습니다."
    end
    LR-->>LS: 좋아요 productId 목록
    LS-->>-LF: 좋아요 productId 목록
    LF->>+PS: 상품 정보 조회 (좋아요 productId 목록)
    PS-->>-LF: 상품 정보 목록
    LF-->>-LA: 200 OK (좋아요 상품 목록 JSON)
    LA-->>-U: 200 OK (좋아요 상품 목록 JSON)
```    

# 🧾 주문 / 결제 (Orders)

## 7. 주문 요청
```mermaid
sequenceDiagram
    actor U as User
    participant OA as OrderApi
    participant OF as OrderFacade
    participant PRS as ProductService
    participant POS as PointService
    participant OS as OrderService
    
    U->>+OA: 주문 요청 (X-USER-ID 헤더, 주문 정보)
    OA->>+OF: 주문 요청 (X-USER-ID 헤더, 주문 정보)
    alt 미로그인, 헤더 정보 없음
        OF-->>OA: 401 UNAUTHORIZED
    end
    
    OF->>+PRS: 상품 정보 조회 (주문 정보)
    alt 상품 미존재
        PRS-->>OA: 404 NOT FOUND + "상품을 찾을 수 없습니다."
    else 상품 재고 부족
        PRS-->>OA: 400 BAD REQUEST + "상품 재고가 부족합니다."
    else 최대 개수 초과
        PRS-->>OA: 400 BAD REQUEST + "최대 개수를 초과했습니다."
    else 상품 판매중이 아님
        PRS-->>OA: 409 CONFLICT + "현재 판매 중이 아닌 상품입니다."
    else 
        PRS-->>OF: 상품 재고 차감
        OF->>+POS: 포인트 차감 요청(유저정보, 주문정보)
        alt 포인트 부족
            POS-->>OA: 400 BAD REQUEST + "포인트가 부족합니다."
        end
        POS-->>-OF: 포인트 차감 성공
        OF->>+OS: 주문 생성 (유저정보, 주문 정보)
        alt 주문 생성 실패
            OS-->>OA: 500 Internal Server Error + "주문 생성 중 오류가 발생했습니다."
        end
        OS-->>-OF: 주문 생성 성공
        OF-->>-OA: 201 CREATED + "주문이 성공적으로 생성되었습니다."
        OA-->>-U: 201 CREATED + "주문이 성공적으로 생성되었습니다."
    end
```    

## 8. 유저의 주문 목록 조회
```mermaid
sequenceDiagram
    actor U as User
    participant OA as OrderApi
    participant OF as OrderFacade
    participant OS as OrderService
    participant LS as LikeService
    
    U->>+OA: 유저의 주문 목록 조회 요청 (X-USER-ID 헤더, ?page, ?size)
    OA->>+OF: 유저의 주문 목록 조회 요청 (X-USER-ID 헤더, ?page, ?size)
    alt 미로그인, 헤더 정보 없음
        OF-->>OA: 401 UNAUTHORIZED
    end
    OF->>+OS: 유저의 주문 목록 조회 (유저정보)
    OS-->>OF: 주문 목록
    OF->>+LS: 각 상품의 총 좋아요 수 조회 (주문 목록의 productId)
    LS-->>OF: 각 상품의 총 좋아요 수
    OF-->>-OA: 주문목록 정보(상품 정보, 각 상품의 총 좋아요 수) 
    OA-->>-U: 200 OK (주문 목록 JSON)
    
```    

## 9. 단일 주문 상세 조회
```mermaid
sequenceDiagram
    actor U as User
    participant OA as OrderApi
    participant OF as OrderFacade
    participant OS as OrderService
    participant PS as ProductService
    participant LS as LikesService
    U->>+OA: 단일 주문 상세 조회 요청 (X-USER-ID 헤더, orderId)
    OA->>+OF: 단일 주문 상세 조회 요청 (X-USER-ID 헤더, orderId)
    alt 미로그인, 헤더 정보 없음
        OF-->>OA: 401 UNAUTHORIZED
    end
    OF->>+OS: 단일 주문 상세 조회 (orderId)
    alt 주문 미존재
        OS-->>OA: 404 NOT FOUND + "주문을 찾을 수 없습니다."
    else 주문 정보 조회 성공
        OS-->>+OF: 주문 상세 정보
        OF->>+PS: 상품 정보 조회 (주문 상세 정보의 productId)
        PS-->>-OF: 상품 정보
        OF->>+LS: 상품 좋아요 수 조회 (주문 상세 정보의 productId)
        LS-->>-OF: 상품 좋아요 수
        OF-->>-OA: 200 OK (주문 상세 정보 JSON, 상품 정보, 좋아요 수)
        OA-->>-U: 200 OK (주문 상세 정보 JSON, 상품 정보, 좋아요 수)
    end
```    
