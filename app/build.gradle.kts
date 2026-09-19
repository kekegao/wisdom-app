import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 后端基础地址：默认指向公网环境（47.111.163.122:8080）。
// 联调可在命令行覆盖：./gradlew assembleDebug -PAPI_BASE_URL=http://192.168.2.2:8083/
// 备用：模拟器宿主机地址（需要时启用下面这行，并注释掉当前生效的那行）
//val apiBaseUrl: String = providers.gradleProperty("API_BASE_URL").getOrElse("http://10.0.2.2:8083/")
// 原局域网联调地址（保留备用，需要时启用下面这行，并注释掉当前生效的那行）
//val apiBaseUrl: String = providers.gradleProperty("API_BASE_URL").getOrElse("http://192.168.2.2:8083/")
val apiBaseUrl: String = providers.gradleProperty("API_BASE_URL").getOrElse("http://47.111.163.122:8080/")

// ==== 发布签名配置 ====
// 密钥路径与密码放在项目根目录 signing.properties（已在 .gitignore 中忽略，不会进 git），
// 也可以用命令行临时覆盖：
//   gradlew assembleRelease -PKEYSTORE_FILE=D:/keys/wisdom.jks -PKEYSTORE_PASSWORD=xxx -PKEY_ALIAS=wisdom -PKEY_PASSWORD=xxx
val signingPropsFile = rootProject.file("signing.properties")
val signingProps = Properties().apply {
    if (signingPropsFile.exists()) {
        signingPropsFile.inputStream().use { load(it) }
    }
}

/** 优先取 -P 传入的值，其次取 signing.properties */
fun signingValue(key: String): String =
    providers.gradleProperty(key).getOrElse("").ifBlank { signingProps.getProperty(key)?.orEmpty() ?: "" }

val keystoreFilePath = signingValue("KEYSTORE_FILE")
val keystorePassword = signingValue("KEYSTORE_PASSWORD")
val signingKeyAlias = signingValue("KEY_ALIAS")
val signingKeyPassword = signingValue("KEY_PASSWORD")

/** 四项都填了才启用签名，否则打出来的 release 包是未签名的 */
val releaseSigningReady = keystoreFilePath.isNotBlank() &&
    keystorePassword.isNotBlank() &&
    signingKeyAlias.isNotBlank() &&
    signingKeyPassword.isNotBlank()

// ==== 版本配置 ====
// 版本号保存在项目根目录 version.properties（会提交到仓库）
//   gradlew bumpVersion                      # versionCode 自动 +1，versionName 不变
//   gradlew bumpVersion -PVERSION_NAME=1.2   # 同时指定新的版本名
//   gradlew assembleRelease -PVERSION_CODE=9 # 临时覆盖，不改文件
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

fun versionValue(key: String, default: String): String =
    providers.gradleProperty(key).getOrElse("").ifBlank { versionProps.getProperty(key)?.orEmpty() ?: default }

val appVersionCode: Int = versionValue("VERSION_CODE", "2").toInt()
val appVersionName: String = versionValue("VERSION_NAME", "1.1")

tasks.register("bumpVersion") {
    // 该任务会写文件，与配置缓存不兼容，显式声明后不会导致构建失败
    notCompatibleWithConfigurationCache("bumpVersion 需要写入 version.properties")
    doLast {
        val nextCode = appVersionCode + 1
        val nextName = providers.gradleProperty("VERSION_NAME").getOrElse("").ifBlank { appVersionName }
        versionPropsFile.writeText("VERSION_CODE=$nextCode\nVERSION_NAME=$nextName\n")
        println("版本号已更新 -> versionCode=$nextCode, versionName=$nextName")
    }
}

android {
    namespace = "com.monkey.wisdom"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.monkey.wisdom"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 后端服务地址，注入到 BuildConfig.API_BASE_URL
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(keystoreFilePath)
                storePassword = keystorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("未配置签名信息（signing.properties 为空），本次 release 包将是未签名 APK")
            }
            optimization {
                enable = false
            }
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // 页面导航
    implementation(libs.androidx.navigation.compose)

    // 网络层：Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
