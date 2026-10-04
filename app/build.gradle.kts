import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Signing details live outside the repo; see keystore.properties.example.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Where Zenith's server is: see server.properties. A value given on the command line wins, which
// is how a test build is pointed at a server on the test PC without changing any file:
//   gradlew assembleDebug -PzenithServerUrl=http://10.0.2.2:8787
val serverProperties = Properties().apply {
    val file = rootProject.file("server.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun serverSetting(name: String, override: String): String =
    ((project.findProperty(override) as String?) ?: serverProperties.getProperty(name) ?: "")
        .trim().filter { it != '"' && it != '\\' }

android {
    namespace = "com.gabriel.zenithui"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // The identity Google Play holds for this app, fixed by the Play listing and unchangeable
        // once published. The Kotlin package above is a separate thing and stays as it is.
        applicationId = "com.gabriel.zenith_Launcher"
        minSdk = 29
        targetSdk = 37
        versionCode = 102
        versionName = "3.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SERVER_URL", "\"${serverSetting("url", "zenithServerUrl")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${serverSetting("googleClientId", "zenithGoogleClientId")}\"")
    }

    signingConfigs {
        create("release") {
            val storePath = keystoreProperties.getProperty("storeFile")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Ships symbols for native code this project builds, so a crash in Play Console would
            // read as function names rather than bare addresses. Nothing is built here today: the
            // one .so in the bundle is a prebuilt from androidx.graphics.path, whose symbols are
            // not ours to ship, which is why Play still mentions them. It costs nothing and covers
            // us the day any native code is added.
            ndk {
                debugSymbolLevel = "FULL"
            }
            if (keystoreProperties.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.fragment)
    // Sign in with Google, through Android's own account chooser.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    // The real org.json for tests on the JVM, where Android's own is only a stub that throws.
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
