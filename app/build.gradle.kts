import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * release 签名参数只从本机 local.properties 读取（该文件不入 git）。
 * 缺失时不创建签名配置，clone 下来没配密钥的人照样能构建（产出未签名包）。
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val releaseKeystoreFile = localProps.getProperty("anchor.release.storeFile")?.let { file(it) }

android {
    namespace = "com.anchor.recovery"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "com.anchor.recovery"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.2.1"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = localProps.getProperty("anchor.release.storePassword")
                keyAlias = localProps.getProperty("anchor.release.keyAlias")
                keyPassword = localProps.getProperty("anchor.release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        // 迁移测试要用 Room 导出的 schema JSON（资产路径 = 数据库类全限定名）。
        // 只挂 debug 与 androidTest、不挂 main：schema 只给调试包和测试包，release 包不打包。
        // 对应地，迁移测试放在 src/testDebug（release 变体读不到 schema，会 FileNotFoundException）。
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/** Room schema 导出目录：schema JSON 入库，供后续版本做迁移测试与变更审查。 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    // Compose UI 测试跑在 JVM（Robolectric）里，这样没有真机也能纳入 :app 门禁；
    // ui-test-manifest 提供宿主 ComponentActivity，只给 debug 变体（测试与调试包同源）。
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // 真机验证（SQLCipher 原生库与 Android Keystore 在 JVM/Robolectric 下不可用，只能跑在这里）
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

/**
 * 内容数据门禁：内容管线（content-tools/build_content.py）的产物必须在构建期自检，
 * 避免缺文章 / 坏 JSON / 计数不一致被打进 APK。
 */
val verifyContentAssets by tasks.registering {
    group = "verification"
    description = "校验 assets/content/*.json 与内容管线产物一致"

    val contentDir = layout.projectDirectory.dir("src/main/assets/content")
    inputs.dir(contentDir).withPropertyName("contentDir")

    doLast {
        val articlesFile = contentDir.file("articles.json").asFile
        val indexFile = contentDir.file("index.json").asFile
        check(articlesFile.isFile) { "缺少内容文件：$articlesFile" }
        check(indexFile.isFile) { "缺少内容文件：$indexFile" }

        val articles = groovy.json.JsonSlurper().parse(articlesFile, "UTF-8") as List<*>
        val index = groovy.json.JsonSlurper().parse(indexFile, "UTF-8") as Map<*, *>
        val categories = index["categories"] as List<*>

        check(articles.size == 71) { "内容文章数应为 71，实际 ${articles.size}" }
        check(index["total"] == 71) { "index.total 应为 71，实际 ${index["total"]}" }

        val ids = articles.mapNotNull { (it as Map<*, *>)["id"] as? String }.toSet()
        check(ids.size == articles.size) { "文章 id 存在重复或缺失" }

        var counted = 0
        categories.forEach { raw ->
            val category = raw as Map<*, *>
            val key = category["key"]
            val articleIds = category["articleIds"] as List<*>
            check(category["count"] == articleIds.size) { "分类 $key 的 count 与 id 数不一致" }
            articleIds.forEach { id -> check(id in ids) { "分类 $key 引用了不存在的文章 id=$id" } }
            counted += articleIds.size
        }
        check(counted == articles.size) { "分类计数合计 $counted 与文章数 ${articles.size} 不一致" }

        val allowedCredibility = setOf("高", "中", "低")
        articles.forEach { raw ->
            val article = raw as Map<*, *>
            val id = article["id"]
            check(article["credibility"] in allowedCredibility) {
                "文章 $id 的可信度非法：${article["credibility"]}"
            }
            val source = article["source"] as? String
            check(source != null && (source.startsWith("http://") || source.startsWith("https://"))) {
                "文章 $id 的 source 非法：$source"
            }
            check(!(article["title"] as? String).isNullOrBlank()) { "文章 $id 标题为空" }
            check(!(article["summary"] as? String).isNullOrBlank()) { "文章 $id 摘要为空" }
            check(!(article["bodyMarkdown"] as? String).isNullOrBlank()) { "文章 $id 正文为空" }
        }

        logger.lifecycle("内容自检通过：${articles.size} 篇文章 / ${categories.size} 个分类")
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verifyContentAssets) }
