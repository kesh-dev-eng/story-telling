plugins {
    java
}

group = "dev.serverguide"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Change this to match the exact Paper API version listed at https://papermc.io/downloads/paper
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

java {
    // Minecraft 26.1+ needs a newer Java than older versions. Change if your server uses a different one.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.processResources {
    filteringCharset = "UTF-8"
}
