plugins {
    kotlin("jvm") version "2.2.21"
    application
}

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
    sourceSets.test { kotlin.srcDir("tests") }
}

dependencies { testImplementation(kotlin("test-junit")) }

application {
    mainClass.set("nuget.MainKt")
    applicationName = "nuget-graph"
}

tasks.test {
    useJUnit()
    testLogging { events("failed", "skipped") }
}
