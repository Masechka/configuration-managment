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
    exclude("**/WindowIntegrationTest.class")
    jvmArgs("-Djava.awt.headless=true")
    testLogging {
        events("failed", "skipped")
    }
}

tasks.register<Test>("guiTest") {
    description = "Проверяет реальное окно Swing; требуется графическая сессия."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/WindowIntegrationTest.class")
    useJUnit()
    jvmArgs("-Djava.awt.headless=false")
    testLogging { events("failed", "skipped") }
    mustRunAfter(tasks.test)
}
