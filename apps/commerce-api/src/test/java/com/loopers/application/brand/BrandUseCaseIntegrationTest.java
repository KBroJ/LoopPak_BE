package com.loopers.application.brand;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BrandUseCaseIntegrationTest {

    @Autowired
    private BrandApplicationService brandApplicationService;
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }


    private static final String NAME = "루퍼스";
    private static final String DESCRIPTION = "백엔드 실력 성장 이직하고 싶다";
    private static final Boolean IS_ACTICE = true;

    @DisplayName("브랜드 생성")
    @Nested
    class create {
        @DisplayName("브랜드 생성 시 브랜드 정보가 반환된다.")
        @Test
        void returnBrand_whenCreateBrand() {

            // arrange

            // act
            BrandInfo result = brandApplicationService.create(NAME, DESCRIPTION, IS_ACTICE);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.id()).isNotNull(),
                () -> assertThat(result.name()).isEqualTo(NAME),
                () -> assertThat(result.description()).isEqualTo(DESCRIPTION),
                () -> assertThat(result.isActive()).isEqualTo(IS_ACTICE)
            );

        }
    }

    @DisplayName("브랜드 정보 조회")
    @Nested
    class brandInfo {

        @DisplayName("존재하지 않는 브랜드 id값으로 정보 조회 시, Optional.empty 가 반환된다.")
        @Test
        void returnOptionalEmpty_whenBrandIdNotExists() {

            // arrange
            Long brandId = 2l;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                brandApplicationService.getBrand(brandId);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

        }

        @DisplayName("존재하는 브랜드 정보를 id값으로 조회 시, 브랜드 정보가 반환된다.")
        @Test
        void returnBrandInfo_whenFindBrandInfo() {

            // arrange
            BrandInfo brandInfo = brandApplicationService.create(NAME, DESCRIPTION, IS_ACTICE);

            // act
            BrandInfo result = brandApplicationService.getBrand(brandInfo.id());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.id()).isEqualTo(brandInfo.id()),
                    () -> assertThat(result.name()).isEqualTo(NAME),
                    () -> assertThat(result.description()).isEqualTo(DESCRIPTION),
                    () -> assertThat(result.isActive()).isEqualTo(IS_ACTICE)
            );

        }

    }
}
