package com.loopers.interfaces.api.example;

import com.loopers.application.example.ExampleFacade;
import com.loopers.application.example.ExampleInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
    외부 세계와 application 계층을 연결해주는 접점
        - HTTP 요청을 받아서 application 계층의 비즈니스 로직을 호출하고,
          그 결과를 HTTP 응답으로 변환하여 반환하는 역할

    Controller 개발 추천 순서(Top-Down 방식)
        1. `DTO` (Data Transfer Object) 정의 (데이터 설계)
           * 무엇을: API가 주고받을 데이터의 모양(구조)을 먼저 정의합니다. "상품을 등록할 때 어떤 정보(이름, 가격 등)가 필요한가?", "등록 후 어떤 정보를 보여줄 것인가?"를
             결정합니다.
           * 이유: API의 가장 기본적인 '재료'입니다. 이 데이터 구조가 명확해야 다음 단계인 API 명세를 정의할 수 있습니다.


   2. `ApiSpec` (API Specification) 정의 (API 명세 설계)
       * 무엇을: 어떤 HTTP Method(GET, POST)와 URL 경로(/api/v1/products)를 가질 것인지, 그리고 1번에서 만든 DTO를 어떻게 사용할 것인지 인터페이스에 명시합니다. Swagger
         같은 API 문서 자동화를 위한 어노테이션도 여기에 붙입니다.
       * 이유: 실제 로직 없이 API의 '껍데기'이자 '공식적인 약속'을 먼저 만드는 것입니다. 이 명세만 봐도 개발자들은 이 API가 어떻게 동작할지 예측할 수 있습니다.


   3. `Controller` 구현 (API 구현)
       * 무엇을: 2번에서 만든 ApiSpec 인터페이스를 `implements` 하여 실제 클래스를 만듭니다. Controller의 역할은 오직 HTTP 요청을 받고, 다음 계층인 Facade에 작업을 넘기는
         것뿐입니다.
       * 이유: 설계도(ApiSpec)에 따라 실제 건물의 뼈대(Controller)를 세우는 단계입니다. 아직 Facade가 없어서 컴파일 에러가 나겠지만, 그건 자연스러운 과정입니다.
*/
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/examples")
public class ExampleV1Controller implements ExampleV1ApiSpec {

    private final ExampleFacade exampleFacade;

    @GetMapping("/{exampleId}")
    @Override
    public ApiResponse<ExampleV1Dto.ExampleResponse> getExample(
        @PathVariable(value = "exampleId") Long exampleId
    ) {
        ExampleInfo info = exampleFacade.getExample(exampleId);
        ExampleV1Dto.ExampleResponse response = ExampleV1Dto.ExampleResponse.from(info);
        return ApiResponse.success(response);
    }
}
