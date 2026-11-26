import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// DashGrid's own release key (keystore.properties is not committed to any public repository):
// the same key signs every update, so an installed DashGrid can be updated in place.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { input -> load(input) }
}

// Car screen mockups for the README (tools/mockups.sh): Paparazzi draws them on the JVM. Only with
// -Pmockups, so ordinary builds and CI don't carry it; it doesn't support the configuration cache here.
val mockups = providers.gradleProperty("mockups").isPresent
if (mockups) apply(plugin = libs.plugins.paparazzi.get().pluginId)

// The fork's released version: raised here, and used for the APK file name below.
val dashgridVersionName = "1.1.1"

android {
    namespace = "net.prototux.aare"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.dashgrid.app"
        // Android 6. Newer features need newer Android: notification channels and foreground-
        // service start (8), car audio by screen capture and the modern Wi-Fi join (10), Shizuku
        // capture (12); older phones fall back or go without. A one-time notice on Android 6 and 7
        // (MainActivity) lists what's limited there.
        minSdk = 23
        targetSdk = 37
        // The released version, raised in the release commit (the release workflow checks that the
        // tag matches): F-Droid builds the tag and reads it from here. versionCode is
        // major * 10000 + minor * 100 + patch. -PversionName / -PversionCode still override it.
        versionCode = 10101
        versionName = dashgridVersionName
        providers.gradleProperty("versionCode").orNull?.let { versionCode = it.toInt() }
        providers.gradleProperty("versionName").orNull?.let { versionName = it }
        // The update check points at the upstream AARE repository, so it is off unless a DashGrid
        // release repository is configured: -Pdashgrid.updateCheck=true turns it back on.
        buildConfigField("boolean", "UPDATE_CHECK", (providers.gradleProperty("dashgrid.updateCheck").orNull == "true").toString())
    }

    signingConfigs {
        // DashGrid's own key: keystore.properties (kept out of public repositories) or the
        // DASHGRID_KEYSTORE* environment variables. Without it the release APK is unsigned.
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        } else {
            val keystore = providers.environmentVariable("DASHGRID_KEYSTORE").orNull
            if (keystore != null) create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("DASHGRID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("DASHGRID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("DASHGRID_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
        // A variant for phones without AARE: it defines android.car.permission.TEMPLATE_RENDERER
        // itself (see src/standalone/AndroidManifest.xml). The default release must not define it,
        // or Android refuses to install it beside AARE, which owns the permission with another
        // certificate. Same application id, version and signing key: pick one variant per phone.
        create("standalone") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.findByName("release")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    if (mockups) sourceSets["test"].kotlin.srcDir("src/mockups/java")
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "META-INF/versions/**")
    }
    lint {
        // a sideloaded app: no Play Store policy checks
        disable += setOf("ProtectedPermissions", "QueryAllPackagesPermission")
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

/**
 * The release APK under the name the project publishes: DashGrid-v<version>.apk.
 * `./gradlew dashgridApk` signs, builds and renames it.
 */
val dashgridVersion = providers.gradleProperty("versionName").orNull ?: dashgridVersionName
tasks.register<Copy>("dashgridApk") {
    group = "build"
    description = "Builds the signed side-by-side APK as DashGrid-v$dashgridVersion.apk"
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(layout.buildDirectory.dir("outputs/dashgrid"))
    rename("app-release\\.apk", "DashGrid-v$dashgridVersion.apk")
}

/** The standalone variant: only for phones where AARE is not installed. */
tasks.register<Copy>("dashgridStandaloneApk") {
    group = "build"
    description = "Builds the standalone signed APK as DashGrid-standalone-v$dashgridVersion.apk"
    dependsOn("assembleStandalone")
    from(layout.buildDirectory.file("outputs/apk/standalone/app-standalone.apk"))
    into(layout.buildDirectory.dir("outputs/dashgrid"))
    rename("app-standalone\\.apk", "DashGrid-standalone-v$dashgridVersion.apk")
}

/** Both APKs, for a release that lets the user pick based on whether AARE is installed. */
tasks.register("dashgridApks") {
    group = "build"
    description = "Builds both signed APKs (side-by-side and standalone)"
    dependsOn("dashgridApk", "dashgridStandaloneApk")
}

dependencies {
    implementation(project(":aap"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.projected)
    implementation(libs.media3.session)
    implementation(libs.kotlinx.coroutines.android)
    // Car audio without the screen-capture consent: through Shizuku (adb rights), or Android's
    // audio-policy interface when installed as a privileged app (tools/magisk).
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    testImplementation(libs.junit)
    // vehicles.json is parsed with the platform's org.json, which unit tests only see as a stub.
    testImplementation(libs.org.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
