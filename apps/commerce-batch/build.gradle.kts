dependencies {
    // add-ons
    implementation(project(":modules:jpa"))         // DB 접근
    implementation(project(":modules:redis"))       // Redis 접근
    implementation(project(":supports:jackson"))    // JSON 처리
    implementation(project(":supports:logging"))    // 로깅
    implementation(project(":supports:monitoring")) // 모니터링
    implementation(project(":apps:commerce-collector"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // test-fixtures
    testImplementation("org.springframework.batch:spring-batch-test")  // 배치 테스트용
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))
    testImplementation(testFixtures(project(":modules:kafka")))
}