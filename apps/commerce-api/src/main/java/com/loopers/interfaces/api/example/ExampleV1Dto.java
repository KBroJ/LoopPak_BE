package com.loopers.interfaces.api.example;

import com.loopers.application.example.ExampleInfo;

/*
    DTO (Data Transfer Object) 정의
        - API가 주고받을 데이터의 모양(구조)을 정의합니다.
        - 예시 조회 결과로 반환할 데이터 구조를 정의합니다.
        - 이 DTO는 API 응답의 형태를 명확히 하고, Swagger 문서화에도 사용됩니다.


    record : 데이터를 불변으로 저장하는 목적의 클래스를 정의할 때 사용합니다.
        - 쓰는 이유
            - DTO는 주로 데이터를 전달하는 용도로 사용되므로, 불변 객체로 정의하여 데이터의 일관성을 유지합니다.
            - `record`를 사용하면 자동으로 `equals()`, `hashCode()`, `toString()` 메서드가 생성되어 편리합니다.

    static
       * 이게 뭔가요?:
            ExampleV1Dto 클래스 안에 Register와 Response라는 클래스를 또 만든 것입니다.
            static이 붙어있으면 '정적 중첩 클래스(Static Nested Class)'라고 부릅니다.
       * 왜 쓰나요?:
           * 강한 연관성 표현: Register와 Response는 오직 ExampleV1 API에서만 사용되는 데이터 구조입니다. 이렇게 안에 묶어둠으로써 "이것들은 ExampleV1 API 세트다" 라는 것을
             명확하게 보여줍니다. 파일 하나로 관련 DTO를 모두 관리할 수 있어 깔끔합니다.
           * 독립성: static으로 선언하면 바깥 클래스(ExampleV1Dto)의 인스턴스 없이도 new ExampleV1Dto.Response() 처럼 독립적으로 객체를 만들 수 있습니다.
                    DTO는 그 자체로 독립적인 데이터 덩어리이므로 static을 붙이는 것이 자연스럽습니다.


  3. from() 정적 팩토리 메소드
       * 이게 뭔가요?: new 키워드를 사용한 생성자 대신, 클래스 스스로 객체를 만들어 반환하는 static 메소드입니다. 디자인 패턴 중 하나이며, from, of, getInstance 등의 이름을
         주로 사용합니다.
       * 왜 쓰나요? (매우 중요!): 계층 간의 의존성을 분리하고 변환의 책임을 명확히 하기 위함입니다.
           * Controller는 Facade로부터 ExampleInfo 객체를 받습니다.
           * 하지만 Controller는 클라이언트에게 ExampleV1Dto.Response 형태로 응답을 보내야 합니다.
           * 이때 Controller가 직접 ExampleInfo의 값을 하나하나 꺼내서 Response DTO를 만드는 로직을 짜는 대신, `Response` DTO에게 "네가 알아서 `ExampleInfo`를 보고 너 자신을
             만들어!" 라고 책임을 위임하는 것입니다.
           * Response.from(info) 이 한 줄로 변환이 끝나므로 Controller의 코드는 매우 깔끔해집니다.

*/
public class ExampleV1Dto {
    public record ExampleResponse(Long id, String name, String description) {
        public static ExampleResponse from(ExampleInfo info) {
            return new ExampleResponse(
                info.id(),
                info.name(),
                info.description()
            );
        }
    }
}
