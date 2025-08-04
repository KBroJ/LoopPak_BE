# 종합 클래스 다이어그램

```mermaid
classDiagram

    class User {
        - Long id
        - String userId
        - Gender gender
        - BigDecimal points

        + addPoints()
        + deductPoints()
    }
    
    class Gender {
        <<enumeration>>
        MALE
        FEMAILE
        OTHER
    }
    
    class Product {
        - Long id
        - Long brandId
        - String name
        - String description
        - BigDecimal price
        - Integer stock
        - Integer maxOrderCount
        - ProductStatus status
        
        + increaseStock(int quantity)
        + decreaseStock(int quantity)
        + activate()
        + inActivate()
        + outOfStock()
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
        + deactivateBrand()
    }
    
    class Like {
        - Long id
        - Long userId
        - Long targetId
        - LikeType type
    }
    class LikeType {
        <<enumeration>>
        PRODUCT
        BRAND
    }
    
    class Order {
        - Long id
        - Long userId
        - List~OrderItem~ orderItems
        - OrderStatus status
        
        + +calculateTotalPrice() long
    }
    
    class OrderItem {
        - Long id
        - Long productId
        - int quantity
        - long price
        
        + getTotalPrice() long
    }
    
    class OrderStatus {
        <<enumeration>>
        PENDING
        PAID
        SHIPPED
        DELIVERED
        CANCELLED
    }
    
    
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
