plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ru.offerfactory.promodisplay.auto_boot_impl"
    compileSdk = 36

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":feature:auto-boot-api"))

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    // workManager
    implementation(libs.androidx.work.runtime.ktx)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
}