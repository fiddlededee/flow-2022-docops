import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    application
}

group = "ru.curs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("com.approvaltests:approvaltests:18.5.0")
    testImplementation(kotlin("test"))
    implementation("org.asciidoctor:asciidoctorj:2.5.7")
    implementation("com.google.guava:guava:21.0")
    implementation("org.languagetool:language-ru:5.6")

}



tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}



