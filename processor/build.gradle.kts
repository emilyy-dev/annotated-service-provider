dependencies {
    implementation(project(":annotated-service-provider"))
}

tasks.withType<Jar> {
    manifest.attributes["Automatic-Module-Name"] = "io.github.emilyydev.asp.processor"
}
