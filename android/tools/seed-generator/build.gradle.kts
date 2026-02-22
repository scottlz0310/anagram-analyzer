plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

application {
    mainClass.set("com.anagram.tools.seedgenerator.MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.51.2.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.named<JavaExec>("run") {
    jvmArgs("-Djdk.xml.entityExpansionLimit=0")
}

tasks.test {
    useJUnit()
}
