plugins {
    kotlin("jvm") version "2.2.21"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    sourceSets.test {
        kotlin.srcDir("tests")
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass.set("shell.MainKt")
    applicationName = "vfs-shell"
}

tasks.test {
    useJUnit()
    jvmArgs("-Djava.awt.headless=true")
    testLogging {
        events("failed", "skipped")
    }
}
