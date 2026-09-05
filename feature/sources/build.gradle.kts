// 音源 feature：KMP 双 target（android + jvm）。
// U11 上收：WebDAV 浏览页/表单页（Toast 经 ui-shared PlatformToast 跨平台）与两个 WebDAV
// ViewModel 进 commonMain（依赖 = :core:common + :core:ui-shared + :core:webdav[已 KMP]）；
// 音源列表页（haze/SAF）与带扫描器的 SourcesViewModel 子类留 androidMain，
// 扫描经 commonMain [LibraryScanPort] 注入（本地 MediaStore/WebDAV 两扫描器为安卓栈）。
// 形态与约束同 :feature:library / :feature:playlist（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.sources"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:webdav"))
            implementation(project(":core:ui-shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            // 列表页 haze 玻璃 + 扫描器（MediaStore/WebDAV，安卓媒体栈）
            implementation(project(":core:media"))
            implementation(libs.haze)
            implementation(libs.haze.blur)
        }
    }
}
