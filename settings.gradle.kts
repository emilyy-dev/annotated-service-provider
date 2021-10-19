rootProject.name = "annotated-service-provider"

include(
    "annotation",
    "processor"
)

project(":annotation").name = rootProject.name
project(":processor").name = "${rootProject.name}-processor"
