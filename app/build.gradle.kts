plugins {
    alias(libs.plugins.hermes.android.application)
    alias(libs.plugins.hermes.android.compose)
    alias(libs.plugins.hermes.hilt)
    alias(libs.plugins.hermes.quality)
}

android {
    namespace = "app.hermes"

    defaultConfig {
        applicationId = "app.hermes"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // M0: no shrinking yet — an R8/proguard config lands with the network layer (M1).
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:brain"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
