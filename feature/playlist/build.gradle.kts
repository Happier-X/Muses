// 播放列表 feature：KMP 双 target（android + jvm）。
// U10 播放列表上收：AddToPlaylistSheet（弹层）、AddToPlaylistViewModel 与歌单详情数据核
// （PlaylistDetailCoreViewModel）进 commonMain（依赖面 = :core:common + :core:ui-shared，
// PlaylistRepository 接口/实现已下沉 :core:common）；haze 玻璃 Page 层与播放依赖
// （PlayerConnection 注入的 PlaylistDetailViewModel/PlaylistsViewModel）留 androidMain。
// 形态与约束同 :core:ui-shared / :feature:library（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.playlist"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:ui-shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            // androidx.lifecycle 2.8+ 的 ViewModel/viewModelScope 为 KMP 工件
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            // Page 层（haze 玻璃）与播放依赖（PlaylistDetailViewModel 注入 PlayerConnection）
            implementation(project(":core:media"))
            implementation(libs.haze)
            implementation(libs.haze.blur)
        }
    }
}
