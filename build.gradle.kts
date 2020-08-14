import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("org.springframework.boot") version ("2.2.6.RELEASE")
//    id("org.springframework.cloud.contract") version ("2.2.2.RELEASE")
    id("io.spring.dependency-management") version ("1.0.9.RELEASE")
//    id("spring-cloud-contract")
    kotlin("jvm") version ("1.3.72")
    kotlin("plugin.spring") version ("1.3.72")
    id("org.sonarqube") version ("3.0")
    id("com.google.cloud.tools.jib") version ("2.2.0")
    jacoco
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-contract-dependencies:2.2.2.RELEASE")
    }
}

group = "com.joinupy.web.mono"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_14

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
//    maven { url 'https://jcenter.bintray.com' }
    gradlePluginPortal()
    mavenCentral()
    jcenter()
    mavenLocal()
}

val javafakerVersion = "0.15"
val archunitJunit5Version = "0.13.1"
val zolandoVersion = "0.25.2"

dependencies {
    //Core
    implementation("org.jetbrains.kotlin:kotlin-reflect")
//    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.github.javafaker:javafaker:${javafakerVersion}")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("com.tngtech.archunit:archunit-junit5-api:${archunitJunit5Version}")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:${archunitJunit5Version}")
    //ERROR
    implementation("org.zalando:problem-spring-webflux:${zolandoVersion}")
    //JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // FIXME [IJP] 30/04/2020: create reflexion
//    implementation("com.fasterxml.jackson.module:jackson-module-afterburner")
    //Web
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.restdocs:spring-restdocs-webtestclient")
    //Mongo
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    //test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        testImplementation("com.ninja-squad:springmockk:2.0.0")
        testImplementation("io.mockk:mockk:1.9.3")
    }
    // Await
    testImplementation("org.awaitility:awaitility-kotlin:4.0.0")
    //wiremock
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")

    //Client
    implementation("com.squareup.retrofit2:retrofit:2.8.1")
    implementation("com.jakewharton.retrofit:retrofit2-reactor-adapter:2.1.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.8.1")

    // Money
    implementation("org.javamoney:moneta:1.3") {
        exclude(module = "moneta-convert")
        exclude(module = "moneta-convert-imf")
        exclude(module = "moneta-convert-ecb")
    }

    //Graphql
    implementation("com.expediagroup:graphql-kotlin-schema-generator:2.1.1")
    implementation("com.expediagroup:graphql-kotlin-spring-server:2.1.1")

    //Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-config")
    implementation("org.springframework.security:spring-security-data")
    implementation("org.springframework.security:spring-security-web")
    implementation("io.jsonwebtoken:jjwt-api:0.11.1")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.1")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.1")
}

tasks.register<JacocoReport>("codeCoverageReport") {
    subprojects {
        val subproject = this
        subproject.plugins.withType<JacocoPlugin>().configureEach {
            subproject.tasks.matching({ it.extensions.findByType<JacocoTaskExtension>() != null }).configureEach {
                val testTask = this
                sourceSets(subproject.sourceSets.main.get())
                executionData(testTask)
            }
            subproject.tasks.matching({ it.extensions.findByType<JacocoTaskExtension>() != null }).forEach {
                rootProject.tasks["codeCoverageReport"].dependsOn(it)
            }
        }
    }
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "13"
    }
}

tasks.withType<BootRun> {
    systemProperties = System.getProperties().entries.associate { it.key?.toString() to it.value }
}

tasks["sonarqube"].dependsOn("test")

sonarqube {
    properties {
        property("http://sonar.kube.join-upy.com")
    }
}