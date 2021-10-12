# Annotated Service Provider
### Define JVM service providers by annotating the provider class directly.
[![Maven Central](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/io/github/emilyy-dev/annotated-service-provider/maven-metadata.xml.svg?label=maven%20central&colorB=brightgreen)](https://search.maven.org/artifact/io.github.emilyy-dev/annotated-service-provider)

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

### Adding it to my project
ASP is released on Maven Central under the coordinates `io.github.emilyy-dev:annotated-service-provider:{version}`. The
current version released is shown at the top of this readme.

Importing ASP to your project:
* Using Gradle
```groovy
plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.emilyy-dev:annotated-service-provider:{version}")
    annotationProcessor("io.github.emilyy-dev:annotated-service-provider:{version}")
}
```
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