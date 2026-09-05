// 曲库 feature：KMP 双 target（android + jvm）。
// U9 曲库 ViewModel/Screen 上收：SongsScreen/AlbumsScreen/ArtistsScreen/详情屏、
// 五个 ViewModel 与 KoinModule 进 commonMain（依赖面 = :core:common + :core:ui-shared，
// 全部已 KMP）；安卓富功能 Page 层（多选/haze 玻璃/跳转 FAB）留 androidMain。
// 形态与约束同 :core:ui-shared（android.kmp.library，双 target，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.library"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            // ViewModel 直依赖的 DAO/Repository/Relations/Mappers 均在 :core:common commonMain（P2b）
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            // androidx.lifecycle 2.8+ 的 ViewModel/viewModelScope 为 KMP 工件
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；viewModel{} DSL 在 koin-core，koinViewModel() 在 compose-viewmodel，
            // 双平台工件；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            // Page 层（多选/haze 玻璃/跳转 FAB）的平台依赖
            implementation(project(":core:media"))
            // 「加入播放列表」长按菜单复用 AddToPlaylistSheet（feature→feature 例外）
            implementation(project(":feature:playlist"))
            // Page 层网格封面（GridCover 全限定名调 coil3.compose.AsyncImage）
            implementation(libs.coil.compose)
            implementation(libs.haze)
            implementation(libs.haze.blur)
        }
    }
}
