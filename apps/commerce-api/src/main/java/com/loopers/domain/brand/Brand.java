package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(name = "brand")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Brand extends BaseEntity {


    private String name;
    private String description;
    private Boolean isActive;

    private Brand(String name, String dscription, Boolean isActive) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 비어있을 수 없습니다.");
        }
        if (dscription == null || dscription.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 설명은 비어있을 수 없습니다.");
        }
        if (isActive == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 활성화 상태는 null일 수 없습니다.");
        }

        this.name = name;
        this.description = dscription;
        this.isActive = isActive;
    }

    public static Brand create(
            String brandName, String brandDescription, Boolean isActive
    ) {
        return new Brand(brandName, brandDescription, isActive);
    }

    public void activeBrand() {
        this.isActive = true;
    }
    public void deactiveBrand() {
        this.isActive = false;
    }

}
