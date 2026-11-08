import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val openaiApiKey = localProperties.getProperty("OPENAI_API_KEY", "").trim()
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.example.ep3"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.ep3"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        
        buildConfigField("String", "OPENAI_API_KEY", "\"\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "OPENAI_API_KEY", quoted(openaiApiKey))
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.glide)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
