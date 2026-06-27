plugins {
    id("io.papermc.paperweight.userdev")
}

dependencies {
    implementation(project(":api"))
    compileOnly(project(":infrastructure:common"))
    api(project(":infrastructure:nms_v26_1_1")) {
        exclude(group = "io.papermc.paper", module = "dev-bundle")
    }
    paperweight.paperDevBundle("26.2.build.+")
}