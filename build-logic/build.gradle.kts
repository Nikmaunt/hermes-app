import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "app.hermes.buildlogic"

// Match the app's JVM target so precompiled convention plugins and consumers
// agree on bytecode level.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // build-logic is compiled by Gradle's embedded Kotlin (2.0.x), but the
        // convention plugins reference AGP/Kotlin/Hilt artifacts built with
        // Kotlin 2.2.0. Accept the newer metadata binary version.
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    // compileOnly: convention plugins compile against these APIs; the actual
    // plugins are resolved at apply-time from the root build (declared there
    // with `apply false`), so we never leak a second copy onto the classpath.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "hermes.android.application"
            implementationClass = "HermesAndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "hermes.android.library"
            implementationClass = "HermesAndroidLibraryConventionPlugin"
        }
        register("kotlinJvm") {
            id = "hermes.kotlin.jvm"
            implementationClass = "HermesKotlinJvmConventionPlugin"
        }
        register("androidCompose") {
            id = "hermes.android.compose"
            implementationClass = "HermesAndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "hermes.hilt"
            implementationClass = "HermesHiltConventionPlugin"
        }
        register("room") {
            id = "hermes.room"
            implementationClass = "HermesRoomConventionPlugin"
        }
        register("quality") {
            id = "hermes.quality"
            implementationClass = "HermesQualityConventionPlugin"
        }
    }
}
