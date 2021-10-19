import java.util.jar.Attributes

plugins {
    java
    signing
    `maven-publish`
    id("com.github.hierynomus.license-base") version "0.16.1"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.hierynomus.license-base")

    val snapshot = false
    project.group = "io.github.emilyy-dev"
    project.version = "2.0.0" + if (snapshot) "-SNAPSHOT" else ""

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withJavadocJar()
        withSourcesJar()
    }

    license {
        header = rootProject.file("LICENSE.txt")
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
                version = project.version.toString()

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
        "check" {
            finalizedBy("licenseMain")
        }

        withType<JavaCompile> {
            options.encoding = Charsets.UTF_8.name()
        }

        withType<Jar> {
            manifest.attributes[Attributes.Name.SEALED.toString()] = true
            manifestContentCharset = Charsets.UTF_8.name()
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
}

tasks {
    withType<Jar> { enabled = false }
    withType<JavaCompile> { enabled = false }
    withType<ProcessResources> { enabled = false }
}
