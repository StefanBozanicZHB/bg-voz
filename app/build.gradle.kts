plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "zhb.company.bgvoz"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "zhb.company.bgvoz"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.pdfbox.android)
    testImplementation(libs.junit)
    testImplementation(libs.pdfbox)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/**
 * Regeneriše ugrađenu rezervnu kopiju reda vožnje iz PDF-a u `src/test/resources`.
 * Pokretanje: ./gradlew :app:regenerateBundledTimetable
 */
tasks.register<JavaExec>("regenerateBundledTimetable") {
    group = "bgvoz"
    description = "Regeneriše data/BundledTimetable.kt iz PDF-a u src/test/resources."
    mainClass.set("zhb.company.bgvoz.BundledTimetableGenerator")
    workingDir = rootProject.projectDir
    classpath = files(
        tasks.named("compileDebugUnitTestKotlin").map { it.outputs.files },
        tasks.named("compileDebugKotlin").map { it.outputs.files },
        configurations.named("debugUnitTestRuntimeClasspath"),
    )
    doFirst { logger.lifecycle("Regenerišem BundledTimetable.kt…") }
}
