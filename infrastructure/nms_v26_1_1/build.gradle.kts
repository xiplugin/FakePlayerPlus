plugins {
    id("io.papermc.paperweight.userdev")
}

dependencies {
    implementation(project(":api"))
    compileOnly(project(":infrastructure:common"))
    implementation(project(":infrastructure:nms_v1_21_11")) {
        exclude(group = "io.papermc.paper", module = "dev-bundle")
    }
    paperweight.paperDevBundle("26.1.1.build.+")
}