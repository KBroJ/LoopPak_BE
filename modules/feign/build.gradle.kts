plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // Feign & OpenFeign
    api("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Resilience4j for Feign
    api("io.github.resilience4j:resilience4j-spring-boot3")
    api("io.github.resilience4j:resilience4j-feign")

    // Jackson for JSON processing
    api("com.fasterxml.jackson.core:jackson-databind")

    // Test fixtures
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
}