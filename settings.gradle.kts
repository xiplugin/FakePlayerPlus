plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "FakePlayerPlus"

include("api")
include("plugin")
include("infrastructure:common")
include("infrastructure:nms_v1_21_11")
include("infrastructure:nms_v26_1_1")