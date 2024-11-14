import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "2.0.21"
	kotlin("plugin.jpa") version "2.0.21"

	id("org.shipkit.shipkit-auto-version") version "2.1.0"
	id("maven-publish")
}

group = "ch.ngns"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.liquibase:liquibase-core")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	implementation("com.fasterxml.woodstox:woodstox-core:7.1.0")
	implementation("org.apache.tika:tika-core:3.0.0")
	implementation("org.apache.tika:tika-parsers:3.0.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

	runtimeOnly("com.h2database:h2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.rest-assured:spring-mock-mvc:5.3.2")
	testImplementation("org.assertj:assertj-core")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")


}

tasks.withType<KotlinCompile> {
	kotlin {
		compilerOptions {
			apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
			languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
			jvmTarget.set(JvmTarget.JVM_17)
			freeCompilerArgs.add("-Xjsr305=strict")
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

sourceSets["main"].resources.srcDir("src/main/schema")

publishing {
	publications {
		create<MavenPublication>("bootJava") {
			artifact(tasks.named("bootJar"))
		}
	}
	repositories {
		maven {
			url = uri("https://maven.pkg.github.com/Engenius-ch/ngns-omnireach-vault")
			credentials {
				username = System.getProperty("GITHUB_USER")
				password = System.getProperty("GITHUB_TOKEN")
			}
		}
	}
}