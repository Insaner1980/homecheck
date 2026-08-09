import dev.detekt.gradle.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.stability.analyzer)
    alias(libs.plugins.owasp.dependency.check)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    javaCompiler =
        javaToolchains.compilerFor {
            languageVersion = JavaLanguageVersion.of(17)
        }
}

android {
    namespace = "com.finnvek.homecheck"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.finnvek.homecheck"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets["androidTest"].assets.directories.add("$projectDir/schemas")

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
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

room {
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    version.set("1.8.0")
    android.set(true)
    coloredOutput.set(false)
    ignoreFailures.set(false)

    reporters {
        reporter(
            ReporterType.CHECKSTYLE,
        )
        reporter(
            ReporterType.PLAIN,
        )
    }

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    ignoreFailures = true
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        checkstyle.required.set(true)
        html.required.set(false)
        markdown.required.set(false)
        sarif.required.set(true)
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON", "SARIF")
    outputDirectory = rootProject.layout.projectDirectory.dir("reports")
    suppressionFiles =
        listOf(
            rootProject.layout.projectDirectory
                .file("config/dependency-check/suppressions.xml")
                .asFile
                .absolutePath,
        )
    failBuildOnUnusedSuppressionRule = true

    data {
        directory =
            providers
                .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
                .orElse(
                    rootProject.layout.projectDirectory
                        .dir(".gradle/dependency-check-data")
                        .asFile
                        .absolutePath,
                ).get()
    }

    autoUpdate =
        providers
            .environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE")
            .map { it.equals("true", ignoreCase = true) || it == "1" || it.equals("yes", ignoreCase = true) }
            .getOrElse(true)

    failBuildOnCVSS =
        providers
            .environmentVariable("DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS")
            .map { it.toFloatOrNull() ?: 7f }
            .getOrElse(7f)

    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    skipTestGroups = true

    analyzers {
        ossIndex {
            enabled = false
        }
    }

    nvd {
        providers
            .environmentVariable("NVD_API_KEY")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { apiKey = it }

        delay =
            providers
                .environmentVariable("NVD_API_DELAY_MS")
                .map { it.toIntOrNull() ?: 6_000 }
                .getOrElse(6_000)

        maxRetryCount =
            providers
                .environmentVariable("NVD_API_MAX_RETRY_COUNT")
                .map { it.toIntOrNull() ?: 5 }
                .getOrElse(5)
    }
}

dependencies {
    lintChecks("com.android.security.lint:lint:1.0.4")

    ktlintRuleset(libs.compose.rules.ktlint)
    detektPlugins(libs.compose.rules.detekt)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.billing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
