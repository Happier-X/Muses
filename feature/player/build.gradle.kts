// 播放 feature：KMP 双 target（android + jvm）。
// U12 播放页 ViewModel 上收：PlayerViewModel/QueueViewModel 进 commonMain，经 commonMain
// [PlaybackPort]（P1 驱动面 + U12 UI 全量面）驱动双端播放栈（安卓 PlayerConnection/Media3，
// 桌面 JvmPlayerPort/VLCJ 待播放页统一时接入）；播放屏/歌词面板（AMLL 渲染、haze、
// Android 资源字体）留 androidMain。队列展示字段由 VM 按曲库组合（QueueRow），
// 端口只暴露 songId 有序集。形态同 :feature:library（android.kmp.library，不升级版本线）。
plugins {
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.muses.player.feature.player"
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
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            // P2a Koin（统一 4.2.0；KMP sourceSets 不支持 platform(BOM)，toml 已显式挂版本）
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            // 播放屏/歌词面板（coil 封面、accompanist lyrics-core 映射、collectAsStateWithLifecycle）
            implementation(libs.coil.compose)
            implementation(libs.accompanist.lyrics.core)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
