plugins {
  id("com.android.application") version "8.11.1"
  // Must match the Kotlin compiler embedded in com.meta.spatial.plugin 0.14.0
  // (spatial-gradle-plugin-impl -> kotlin-compiler-embeddable:2.2.0); a 2.1.x
  // plugin fails with an IncrementalCompilationFeatures NoSuchMethodError.
  id("org.jetbrains.kotlin.android") version "2.2.0"
  id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
  id("com.meta.spatial.plugin") version "0.14.0"
}

android {
  namespace = "mn.vrguardian.student"
  compileSdk = 34

  defaultConfig {
    applicationId = "mn.vrguardian.student"
    minSdk = 34 // Horizon OS is Android 14 (API 34)
    targetSdk = 34
    versionCode = 1
    versionName = "0.1"
  }

  packaging { resources.excludes.add("META-INF/LICENSE") }

  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

dependencies {
  val spatialSdk = "0.14.0"

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
  implementation(platform("androidx.compose:compose-bom:2024.09.03"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.material3:material3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

  // Meta Spatial SDK
  implementation("com.meta.spatial:meta-spatial-sdk:$spatialSdk")
  implementation("com.meta.spatial:meta-spatial-sdk-vr:$spatialSdk")
  implementation("com.meta.spatial:meta-spatial-sdk-compose:$spatialSdk")
  implementation("com.meta.spatial:meta-spatial-sdk-toolkit:$spatialSdk")
  debugImplementation("com.meta.spatial:meta-spatial-sdk-ovrmetrics:$spatialSdk")

  testImplementation("junit:junit:4.13.2")
}
