/*
 * Copyright (c) 2022-2023, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */

plugins {
    java
    `maven-publish`
}

group = "org.e2immu"


repositories {
    maven {
        url = uri(project.findProperty("codeartifactUri") as String)
        credentials {
            username = "aws"
            password = project.findProperty("codeartifactToken") as String
        }
    }
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("org.e2immu:e2immu-external-support:${version}")
    implementation("org.e2immu:e2immu-internal-util:${version}")
    implementation("org.e2immu:e2immu-internal-graph:${version}")
    implementation("org.e2immu:e2immu-cst-api:${version}")
    implementation("org.e2immu:e2immu-cst-analysis:${version}")
    implementation("org.e2immu:e2immu-cst-impl:${version}")
    implementation("org.e2immu:e2immu-cst-io:${version}")
    implementation("org.e2immu:e2immu-cst-print:${version}")
    implementation("org.e2immu:e2immu-inspection-api:${version}")
    implementation("org.e2immu:e2immu-inspection-parser:${version}")
    implementation("org.e2immu:e2immu-inspection-resource:${version}")
    implementation("org.e2immu:e2immu-java-parser:${version}")
    implementation("org.e2immu:e2immu-java-bytecode:${version}")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.apiguardian:apiguardian-api:1.1.2")
    implementation("org.junit.platform:junit-platform-commons:1.9.3")

    testImplementation("org.jetbrains:annotations:24.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.8")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            url = uri(project.findProperty("publishUri") as String)
            credentials {
                username = project.findProperty("publishUsername") as String
                password = project.findProperty("publishPassword") as String
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "language-inspection-integration of e2immu analyser"
                description = "Static code analyser focusing on modification and immutability. " +
                        "This module integrates all inspection-related tasks."
                url = "https://e2immu.org"
                scm {
                    url = "https://github.com/e2immu"
                }
                licenses {
                    license {
                        name = "GNU Lesser General Public License, version 3.0"
                        url = "https://www.gnu.org/licenses/lgpl-3.0.html"
                    }
                }
                developers {
                    developer {
                        id = "bnaudts"
                        name = "Bart Naudts"
                        email = "bart.naudts@e2immu.org"
                    }
                }
            }
        }
    }
}
