# Annotated Service Provider
### Define JVM service providers by annotating the provider class directly.
[![Maven Central](https://img.shields.io/maven-central/v/io.github.emilyy-dev/annotated-service-provider?color=yellowgreen&label=maven%20central)](https://search.maven.org/artifact/io.github.emilyy-dev/annotated-service-provider)
[![Snapshot](https://img.shields.io/nexus/s/io.github.emilyy-dev/annotated-service-provider?label=snapshot&server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/emilyy-dev/annotated-service-provider/)
[![License](https://img.shields.io/github/license/emilyy-dev/annotated-service-provider?color=blue)](https://github.com/emilyy-dev/annotated-service-provider/blob/main/LICENSE.txt)

This annotation processor will add to the class-path services deployment (`META-INF/services/`) provider types that are
annotated with the `@Provides` annotation.

### Example usage
```java
package io.github.emilyydev.asp.demo;

import io.github.emilyydev.asp.Provides;
import java.sql.Driver;

@Provides(Driver.class)
public class SqlDriverImpl implements Driver {
  // implementation...
}
```
The annotation processor will read the class, ensure it meets the required criterion specified by the
[ServiceLoader documentation](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) and add the
provider type to the corresponding service file `META-INF/services/java.sql.Driver` as a new entry, in case that same
jar file provides multiple providers for the same service.

### Adding it to your project
ASP is released on Maven Central under the coordinates `io.github.emilyy-dev:annotated-service-provider:{version}`. The
current version released is shown at the top of this readme. Snapshot builds can be found in OSS sonatype
(https://s01.oss.sonatype.org/content/repositories/snapshots/)

Importing ASP to your project:
* Using Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.emilyy-dev:annotated-service-provider:{version}")
    annotationProcessor("io.github.emilyy-dev:annotated-service-provider:{version}")
}
```
> Note: if your project uses Kotlin you need to use [**`kapt`**](https://kotlinlang.org/docs/kapt.html) to use
> annotation processors instead.

* Using Maven
```xml
<project>
    <!--  ...-->
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.1</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>io.github.emilyy-dev</groupId>
                                <artifactId>annotated-service-provider</artifactId>
                                <version>{version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <dependencies>
        <dependency>
            <groupId>io.github.emilyy-dev</groupId>
            <artifactId>annotated-service-provider</artifactId>
            <version>{version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

### License
This project is licensed under the MIT license.
