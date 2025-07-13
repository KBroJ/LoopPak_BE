import org.gradle.api.Project.DEFAULT_VERSION
import org.springframework.boot.gradle.tasks.bundling.BootJar

/** --- configuration functions --- */
// fun getGitHash() : Git의 최근 기록(commit hash)을 가져오는 함수
fun getGitHash(): String {
    return runCatching {    // runCatching : {} 안의 코드 실행, 실행 시 오류가 나면 스킵(프로그램 멈추지 않음)
        providers.exec {    // providers.exec { ... }: Gradle이 터미널(명령 프롬프트) 명령어를 실행
            commandLine("git", "rev-parse", "--short", "HEAD") // 위 exec를 통해 실행할 실제 명령어입니다 => git rev-parse --short HEAD 라고 입력한것과 같음
        }
            /*
                명령어 실행 결과를 가공
                    1) .standardOutput: 명령어의 실행 결과를 가져옴
                    2) .asText: 그 결과를 텍스트(String) 형태로 바꿈
                    3) .get(): 텍스트 값을 최종적으로 얻어냅니다.
                    4) .trim(): 혹시 모를 앞뒤의 공백을 제거합니다.
            */
            .standardOutput.asText.get().trim()
    }.getOrElse { "init" } // 만약 Git이 설치되지 않았거나, .git 폴더가 없는 등 에러가 발생하면, 대신 "init" 이라는 기본값을 사용
}

/** --- project configurations --- */
// 프로젝트에 필요한 핵심 기능 설정
plugins {
    java                                        // 이 프로젝트는 java 프로젝트
    id("org.springframework.boot") apply false  // 스프링 부트 기능 사용, apply false : 여기서는 직접 적용하지 말고, 나중에 하위 프로젝트에서 골라서 적용
    id("io.spring.dependency-management")       // 라이브러리들의 버전을 쉽게 관리해주는 기능
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)    // 이 프로젝트는 자바 21 버전으로 만들겠다고 설정
    }
}

// 모든 프로젝트에 대한 공통 규칙
allprojects {
    val projectGroup: String by project // 코틀린의 '위임 속성(Delegated Property)' 이라는 고급 기능 : 어딘가에 있는 projectGroup이란 이름의 설정을 찾아서 이 변수에 자동으로 넣어줘" 라는 뜻
    group = projectGroup                                                    // 프로젝트의 그룹 이름 (보통 회사 도메인을 거꾸로 씀) 설정
    /*
        if (version == DEFAULT_VERSION): "만약 버전이 한번도 설정된 적 없는 기본 상태라면
            getGitHash() 함수를 호출해서 Git commit hash를 버전으로 사용
            만약 버전이 gradle.properties 등에서 수동으로 지정되었다면(기본 상태가 아니라면), 그 지정된 버전을 그대로 사용
    */
    version = if (version == DEFAULT_VERSION) getGitHash() else version     // 프로젝트의 버전(git 기록 번호가 버전이 되도록 설정)

    // 라이브러리를 다운로드할 저장소 지정
    repositories {
        mavenCentral()
    }
}

// apps, modules 폴더 안에 있는 모든 하위 프로젝트(모듈)에 적용될 규칙 설정
subprojects {
    // 모든 하위 프로젝트에 자바, 스프링 부트 등의 기능을 실제로 적용
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    dependencyManagement {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${project.properties["springCloudDependenciesVersion"]}")
        }
    }

    dependencies {
        // Web
        runtimeOnly("org.springframework.boot:spring-boot-starter-validation")
        // Spring
        implementation("org.springframework.boot:spring-boot-starter")
        // Serialize
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        // Lombok
        implementation("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        // Test
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        // testcontainers:mysql 이 jdbc 사용함
        testRuntimeOnly("com.mysql:mysql-connector-j")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("com.ninja-squad:springmockk:${project.properties["springMockkVersion"]}")
        testImplementation("org.mockito:mockito-core:${project.properties["mockitoVersion"]}")
        testImplementation("org.instancio:instancio-junit:${project.properties["instancioJUnitVersion"]}")
        // Testcontainers
        testImplementation("org.springframework.boot:spring-boot-testcontainers")
        testImplementation("org.testcontainers:testcontainers")
        testImplementation("org.testcontainers:junit-jupiter")
    }


    /*
        실행 파일 만들기 설정
        modules 폴더의 프로젝트들은 '레고 블록'이고, apps 폴더의 프로젝트는 이 블록들을 조립한 '완성된 레고 자동차'라고 생각
    */
    // 기본 규칙: 모든 하위 프로젝트는 기본적으로 **실행 불가능한 라이브러리 파일(Jar)**만 만들도록 설정합니다 (BootJar는 비활성화).
    tasks.withType(Jar::class) { enabled = true }
    tasks.withType(BootJar::class) { enabled = false }

    // 특별 규칙: apps 폴더 안의 프로젝트에 대해서는 **행 가능한 파일(BootJar)**로 만들고, 일반 Jar는 만들지 않습니다.
    configure(allprojects.filter { it.parent?.name.equals("apps") }) {
        tasks.withType(Jar::class) { enabled = false }
        tasks.withType(BootJar::class) { enabled = true }
    }


// 테스트 및 코드 커버리지 설정
    // 테스트를 실행할 때의 환경을 설정
    tasks.test {
        /*
            maxParallelForks : 테스트를 실행할 때, 프로세스를 몇 개까지 동시에 사용할지 정하는 옵션
                1로 설정하면 테스트들이 순서대로 하나씩 실행됩니다.
                2 이상으로 설정하면 여러 테스트가 동시에 병렬로 실행됩니다.
                1로 설정하는 이유는, 여러 테스트가 동시에 실행되면서 서로에게 영향을 주는
                (예: DB 상태를 바꾸는) 경우를 방지하여 테스트의 안정성을 높이기 위함
        */
        maxParallelForks = 1
        useJUnitPlatform()                                                  // 테스트 실행 도구 설정 : JUnit Platform
        // systemProperty : 테스트가 실행되는 동안에만 임시로 적용될 시스템 환경 변수
        systemProperty("user.timezone", "Asia/Seoul")        // 시간대는 서울 기준
        systemProperty("spring.profiles.active", "test")    // 스프링 부트에게 "지금은 테스트 상황이니, 일반 설정(application.yml) 말고 테스트용 설정(application-test.yml)을 사용해!" 라고 알려줌
        /*
            테스트를 실행하는 자바 가상 머신(JVM)에 직접 옵션을 전달합니다.
             -Xshare:off는 클래스 데이터 공유(Class Data Sharing) 기능을 끄는 것입니다.
             이 기능은 원래 자바 앱 실행 속도를 높여주지만, 몇몇 테스트 도구(특히 Mockito 같은 목(mock) 라이브러리)와 충돌을 일으키는 경우가 있습니다.
             그래서 테스트의 안정성을 위해 이 기능을 꺼두는 것입니다.
         */
        jvmArgs("-Xshare:off")
    }

    // 코드 커버리지 (내가 작성한 테스트가 실제 코드를 얼마나 실행해보는지)를 측정하는 도구(Jacoco)의 설정
    tasks.withType<JacocoReport> {
        mustRunAfter("test")
        executionData(fileTree(layout.buildDirectory.asFile).include("jacoco/*.exec"))
        reports {
            xml.required = true
            csv.required = false
            html.required = false
        }
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it)
                    },
                ),
            )
        }
    }
}

// module-container 는 task 를 실행하지 않도록 한다.
project("apps") { tasks.configureEach { enabled = false } }
project("modules") { tasks.configureEach { enabled = false } }
project("supports") { tasks.configureEach { enabled = false } }
