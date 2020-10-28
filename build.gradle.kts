plugins {
    kotlin("multiplatform") version "1.4.10"
    `maven-publish`
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
        val jvmMain by getting{
            dependencies {
                api("io.ktor:ktor-client-cio:$ktorVersion")
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
        val jsMain by getting{
            dependencies {
                api("io.ktor:ktor-client-js:$ktorVersion")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    val vcs = "https://github.com/altavir/ktor-client-sse"

    // Process each publication we have in this project
    publications.filterIsInstance<MavenPublication>().forEach { publication ->

        publication.pom {
            name.set(project.name)
            description.set(project.description)
            url.set(vcs)

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("MIPT-NPM")
                    name.set("MIPT nuclear physics methods laboratory")
                    organization.set("MIPT")
                    organizationUrl.set("http://npm.mipt.ru")
                }

            }
            scm {
                url.set(vcs)
                tag.set(project.version.toString())
            }
        }
    }

    val githubUser: String? by project
    val githubToken: String? by project

    if (githubUser != null && githubToken != null) {
        project.logger.info("Adding github publishing to project [${project.name}]")
        repositories {
            maven {
                name = "github"
                url = uri("https://maven.pkg.github.com/altavir/ktor-client-sse/")
                credentials {
                    username = githubUser
                    password = githubToken
                }
            }
        }
    }
}