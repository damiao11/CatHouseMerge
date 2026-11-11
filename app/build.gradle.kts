plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.cathousemerge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cathousemerge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures.compose = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/game-assets"))
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/game-res"))
}

val prepareGameAssets by tasks.registering(Sync::class) {
    from(rootProject.file("android-assets/rooms")) { into("rooms") }
    from(rootProject.file("android-assets/scenes")) { into("scenes") }
    from(rootProject.file("docs/data")) {
        include("*.json")
        into("content")
    }
    into(layout.buildDirectory.dir("generated/game-assets"))
}

val prepareGameResources by tasks.registering(Sync::class) {
    from(rootProject.file("android-assets/drawable")) { into("drawable-nodpi") }
    into(layout.buildDirectory.dir("generated/game-res"))
}

tasks.named("preBuild") { dependsOn(prepareGameAssets, prepareGameResources) }

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
