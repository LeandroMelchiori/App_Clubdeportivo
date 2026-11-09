import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Logica de negocio pura (cobros, vencimientos y validaciones) que usan la app y el backend.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    testImplementation(libs.junit)
}
