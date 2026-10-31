plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "dev.j6n.still"
    compileSdk = 35
    val buildNumber = (providers.gradleProperty("stillVersionCode").orNull ?: "1").toInt()
    val buildVersion = providers.gradleProperty("stillVersionName").orNull ?: "0.1.0"
    defaultConfig {
        applicationId = "dev.j6n.still"
        minSdk = 29
        targetSdk = 35
        versionCode = buildNumber
        versionName = buildVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("distribution") {
            val encoded = System.getenv("STILL_KEYSTORE_BASE64")
            if (!encoded.isNullOrBlank()) {
                val keystoreFile = layout.buildDirectory.file("signing/still-release.jks").get().asFile
                keystoreFile.parentFile.mkdirs()
                keystoreFile.writeBytes(java.util.Base64.getDecoder().decode(encoded))
                storeFile = keystoreFile
                storePassword = System.getenv("STILL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STILL_KEY_ALIAS")
                keyPassword = System.getenv("STILL_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("distribution")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
