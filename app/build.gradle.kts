import org.gradle.api.Project
import java.io.File
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

private object AppSigning {

    data class Config(
        val storeFile: File,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String
    )

    fun loadOptional(project: Project): Result<Config> = runCatching {
        val properties = Properties().also { properties ->
            val file = project.rootProject.file("signing.properties")
            if (file.exists()) {
                file.reader().use { properties.load(it) }
            }
        }
        val storeFile = project.rootProject.file("signing.keystore")
        val storeBin = getEnvOrProperty(properties, "SIGNING_STORE_BIN")
        val storePassword = getEnvOrProperty(properties, "SIGNING_STORE_PASSWORD")
        val keyAlias = getEnvOrProperty(properties, "SIGNING_KEY_ALIAS")
        val keyPassword = getEnvOrProperty(properties, "SIGNING_KEY_PASSWORD")

        storeFile.writeBytes(Base64.getDecoder().decode(storeBin))
        Config(storeFile, storePassword, keyAlias, keyPassword)
    }

    private fun getEnvOrProperty(properties: Properties, key: String): String {
        return System.getenv(key)
            ?.takeIf(String::isNotBlank)
            ?: properties.getProperty(key)?.takeIf(String::isNotBlank)
            ?: error("$key is not configured")
    }
}

android {
    namespace = "io.github.rosemoe.arcaeaScores"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "io.github.rosemoe.arcaeaScores"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 20000
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        AppSigning.loadOptional(project)
            .onSuccess { config ->
                create("general") {
                    storeFile = config.storeFile
                    storePassword = config.storePassword
                    keyAlias = config.keyAlias
                    keyPassword = config.keyPassword
                    enableV1Signing = true
                    enableV2Signing = true
                }
                buildTypes.configureEach {
                    signingConfig = signingConfigs.getByName("general")
                }
            }
            .onFailure {
                logger.info("App signing is not configured; using the default signing configuration.")
            }
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

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
