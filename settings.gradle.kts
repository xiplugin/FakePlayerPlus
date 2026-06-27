plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "FakePlayerPlus"

include("api")
include("plugin")
include("nms:nms_v1_21_11")
include("nms:nms_v26_1_1")