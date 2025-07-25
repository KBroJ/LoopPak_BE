```mermaid
erDiagram

    USER {
        bigint id PK
        varchar userId
        varchar password
        varchar gender
        varchar birthDate
        varchar email
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
    }
    
    POINT {
        bigint id PK
        varchar userId FK
        bigint point
        datetime createdAt
        datetime updatedAt
    }
    
    BRAND {
        bigint id PK
        varchar brandId
        varchar name
        varchar description
        varchar logoUrl
        boolean isActive
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
    }
    
    PRODUCT {
        bigint id PK
        bigint brandId FK
        varchar productId
        varchar name
        bigint price
        bigint stock
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
    }

    LIKE {
        bigint productId PK, FK
        bigint userId PK, FK
        datetime createdAt
    }
    
    ORDER {
        bigint id PK
        bigint userId FK
        bigint productId FK
        varchar orderId
        bigint quantity
        bigint totalPrice
        varchar status
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
    }
    
    USER ||..|| POINT : userId
    BRAND ||..o{ PRODUCT : brandId
    PRODUCT ||..o{ LIKE : productId
    PRODUCT ||..o{ ORDER : productId
    USER ||..o{ ORDER : userId
    USER ||..o{ LIKE : userId

```
---
## mermaid - erDiagram 연결 설명
- |o : 선택
- || : 필수
- { : 다수
- -- : 식별관계
- .. : 비식별관계

