import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "dev.shadowbound"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

ktor {
    fatJar {
        archiveFileName.set("shadowbound-server.jar")
    }
}

tasks.withType<ShadowJar> {
    archiveFileName.set("shadowbound-server.jar")
    destinationDirectory.set(file("deploy"))
    manifest {
        attributes["Main-Class"] = "io.ktor.server.netty.EngineMain"
    }
}

val copyGameFiles by tasks.registering(Copy::class) {
    from("static")
    into("deploy/static")
}

tasks.shadowJar {
    finalizedBy(copyGameFiles)
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED"
    )
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    // implementation(libs.ktor.serialization.kotlinx.protobuf)
    implementation(libs.ktor.server.websockets)

    implementation(libs.mongodb.driver.kotlin.coroutine)
    implementation(libs.mongodb.bson.kotlinx)
    implementation(libs.library.bcrypt)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
