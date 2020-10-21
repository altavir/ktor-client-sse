plugins {
    kotlin("multiplatform") version "1.4.10"
}

group = "ru.mipt.npm"
version = "0.1.0"

repositories {
    jcenter()
    mavenCentral()
}

val ktorVersion: String = "1.4.1"

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    sourceSets {
        commonMain {
            dependencies {
                api("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.6.1")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}