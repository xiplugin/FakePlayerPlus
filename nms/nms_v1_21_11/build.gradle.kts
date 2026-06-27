plugins {
    id("io.papermc.paperweight.userdev")
}

dependencies {
    implementation(project(":api"))
    paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.21.11-R0.1-SNAPSHOT")
}