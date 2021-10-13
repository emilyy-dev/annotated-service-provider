plugins {
    `java-library`
    signing
    `maven-publish`
    id("com.github.hierynomus.license-base") version "0.16.1"
}

project.group = "io.github.emilyy-dev"
project.version = "1.0.2"
val snapshot: Boolean = true

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains", "annotations", "22.0.0")
}

license {
    header = file("LICENSE.txt")
    encoding = Charsets.UTF_8.name()
    mapping("java", "DOUBLESLASH_STYLE")
    include("**/*.java")
}

publishing {
    repositories {
        maven {
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (snapshot) snapshotsUrl else releasesUrl

            val ossrhUsername = findProperty("ossrh.user") ?: return@maven
            val ossrhPassword = findProperty("ossrh.password") ?: return@maven
            credentials {
                username = ossrhUsername as String
                password = ossrhPassword as String
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString() + if (snapshot) "-SNAPSHOT" else ""

            pom {
                packaging = "jar"
                name.set("Annotated Service Provider")
                description.set("Define service providers in META-INF/services by annotating them directly")
                url.set("https://github.com/emilyy-dev/annotated-service-provider")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/mit-license.php")
                    }
                }

                developers {
                    developer {
                        id.set("emilyy-dev")
                        url.set("https://github.com/emilyy-dev")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/emilyy-dev/annotated-service-provider.git")
                    developerConnection.set("scm:git:ssh://github.com:emilyy-dev/annotated-service-provider.git")
                    url.set("https://github.com/emilyy-dev/annotated-service-provider/tree/main")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks {
    check { finalizedBy(licenseMain) }

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
    }

    withType<Jar> {
        manifestContentCharset = Charsets.UTF_8.name()
        manifest.attributes["Automatic-Module-Name"] = "io.github.emilyydev.asp"
        metaInf {
            from(license.header) { into("${project.group}/${project.name}") }
        }
    }

    withType<Javadoc> {
        val standardOptions = options as StandardJavadocDocletOptions
        standardOptions.links("https://docs.oracle.com/javase/8/docs/api/")
        if (JavaVersion.current().isJava9Compatible) {
            standardOptions.addBooleanOption("html5", true)
        }
    }
}
