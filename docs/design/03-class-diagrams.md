# AsIs

```mermaid
classDiagram

    class User {
        - id: Long
        - userId: String
        - gender: Gender
        - birthDate: String
        - email: String
    }
    
    class Point {
        - id: Long
        - userId: Long
        - amount: BigDecimal
    }
    
    class Brand {
        - id: Long
        - brandId: String
        - name: String
        - description: String
        - logoUrl: String
        - isActive: Boolean
        - createdAt: LocalDateTime
        - updatedAt: LocalDateTime
        - deletedAt: LocalDateTime
    }
    
    class Product {
        - id: Long
        - Brand brand
        - productId: String
        - name: String
        - description: String
        - price: BigDecimal
        - stock: Integer
        - maxOrderCount: Integer
        - status: String
        - createdAt: LocalDateTime
        - updatedAt: LocalDateTime
        - deletedAt: LocalDateTime
    }
    
    class Like {
        - id: Long
        - userId: Long
        - productId: Long
        - createdAt: LocalDateTime
    }
    
    class Order {
        - id: Long
        - orderId: String
        - userId: Long
        - productId: Long
        - quantity: Integer
        - totalPrice: BigDecimal
        - status: String
        - createdAt: LocalDateTime
        - updatedAt: LocalDateTime
    }

    User "1" -- "1" Point : 포인트 소유
    User "1" -- "0..*" Order : 유저는 다수의 주문 가능
    User "1" -- "0..*" Like : 유저는 다수의 상품에 좋아요 가능

    Brand "1" -- "0..*" Product : 브랜드는 다수의 상품 소유

    Product "1" -- "0..*" Order : 상품은 다수의 주문 수용
    Product "1" -- "0..*" Like : 상품은 다수의 좋아요 가능
    
```

---
# ToBe
## 종합 클래스 다이어그램
```mermaid
classDiagram

    class User {
        - Long id
        - String userId
        - Gender gender
        - Point point
    }
    
    class Gender {
        <<enumeration>>
        MALE
        FEMAILE
        OTHER
    }
    
    class Point {
        - Long id
        - User user
        - BigDecimal amount
        
        + addPoints()
        + deductPoints()
    }
    
    class Product {
        - Long id
        - String name
        - String description
        - BigDecimal price
        - Integer stock
        - Integer maxOrderCount
        - ProductStatus status
        - Brand brand
        
        + updateStock()
        + changeStatus()
    }
    
    class ProductStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        OUT_OF_STOCK
    }
    
    class Brand {
        - Long id
        - String name
        - String description
        - String logoUrl
        - Boolean isActive
        
        + activateBrand()
    }
    
    class Like {
        - Long id
        - User user
        - Long targetId
        - LikeType type
        
        + addLike()
        + removeLike()
    }
    
    class LikeType {
        <<enumeration>>
        PRODUCT
        BRAND
    }
    
    class Order {
        - Long id
        - User user
        - List<OrderItem> orderItems
        - BigDecimal totalPrice
        - OrderStatus status
        - LocalDateTime orderDate
        
        + addOrderItem()
        + changeStatus()
    }
    
    class OrderItem {
        - Long id
        - Product product
        - Integer quantity
        - BigDecimal price
    }
    
    class OrderStatus {
        <<enumeration>>
        PENDING
        COMPLETED
        CANCELLED
    }
    
    
    User "1" -- "1" Point : 포인트 소유
    
    %% 여러 상품(N)은 하나의 브랜드(1)에 소속
    Product "N" --> "1" Brand : 소속
    
    %% 사용자(1)는 '좋아요'를 누르지 않거나 여러 개(0..*) 누를 수 있음
    User <-- "0..*" Like : 좋아요
    %% 상품/브랜드는 0번 이상 '좋아요'를 받을 수 있음
    Product <-- "0..*" Like : 좋아요
    Brand <-- "0..*" Like : 좋아요
    
    Order "1" -- "0..*" OrderItem : 주문 항목
    OrderItem "1" -- "1" Product : 상품
    Order "1" -- "1" User : 주문자
    
    User *-- Gender : 성별
    Product *-- ProductStatus : 상품상태
    Like *-- LikeType : 카테고리
    Order *-- OrderStatus : 주문상태
```

---
### 1. 가시성 (Visibility):
* 클래스 멤버(속성, 메서드)에 대한 접근 수준을 나타내는 기호입니다.
* +: public (공개) - 어디서든 접근 가능
* -: private (비공개) - 해당 클래스 내에서만 접근 가능
* #: protected (보호) - 해당 클래스와 자식 클래스에서 접근 가능
* ~: package / default (패키지) - 동일 패키지 내에서만 접근 가능 (Java의 기본 접근 지정자)
### 2. 관계 (Relationships)
* 표현: 실선 (-----)
* 예: 고객은 주문을 한다. (Customer --- Order)
* 다중성 (Multiplicity/Cardinality): 관계에 참여하는 객체의 수를 나타냅니다. 선의 양 끝에 표기합니다.
* 1: 정확히 하나
* 0..1: 0 또는 하나 (선택적)
* *: 0 또는 그 이상 (다수)
* 1..*: 1 또는 그 이상
* m..n: m개에서 n개 사이
