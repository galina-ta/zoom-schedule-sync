plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.32"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    val poiVersion = "3.12"
    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    implementation("org.apache.poi:poi-scratchpad:$poiVersion")

    val googleVersion = "1.23.0"
    implementation( "com.google.api-client:google-api-client:$googleVersion")
    implementation( "com.google.oauth-client:google-oauth-client-jetty:$googleVersion")
    implementation( "com.google.apis:google-api-services-calendar:v3-rev305-1.23.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "zoom.schedule.sync.AppKt"
}

tasks.getByName("run") {
    setProperty("standardInput", System.`in`)
}