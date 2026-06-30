package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val logoUrl: String,
    val streamUrl: String,
    val backupUrl1: String = "",
    val backupUrl2: String = "",
    val isFeatured: Boolean = false,
    val isVisible: Boolean = true,
    val orderIndex: Int = 0,
    val viewCount: Int = 0
)

@Entity(tableName = "live_matches")
data class LiveMatchEntity(
    @PrimaryKey val id: String,
    val team1Name: String,
    val team1Logo: String,
    val team2Name: String,
    val team2Logo: String,
    val matchTime: Long, // timestamp
    val status: String, // UPCOMING, LIVE, FINISHED
    val streamUrl: String
)

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey val id: String,
    val message: String,
    val isMarquee: Boolean = false,
    val type: String = "NEWS", // NEWS, NOTICE, BREAKING
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_configs")
data class AdConfigEntity(
    @PrimaryKey val id: String = "global_ads",
    val enabled: Boolean = true,
    val bannerAdId: String = "ca-app-pub-3940256099942544/6300978111",
    val interstitialAdId: String = "ca-app-pub-3940256099942544/1033173712",
    val nativeAdId: String = "ca-app-pub-3940256099942544/2247696110",
    val rewardedAdId: String = "ca-app-pub-3940256099942544/5224354917",
    val customImageAdUrl: String = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800&auto=format&fit=crop&q=60",
    val customImageClickUrl: String = "https://nekbd.com"
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String = "global_settings",
    val appName: String = "NEKBD IPTV",
    val appLogo: String = "https://images.unsplash.com/photo-1540747737956-37872176751e?w=100&auto=format&fit=crop&q=60",
    val maintenanceMode: Boolean = false,
    val forceUpdate: Boolean = false,
    val minVersion: Int = 1,
    val contactInfo: String = "sobujr09@gmail.com",
    val privacyPolicy: String = "https://nekbd.com/privacy",
    val primaryColor: String = "#E50914", // Sports Red
    val secondaryColor: String = "#1A1A1A"
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String,
    val favoritedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val channelId: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val playbackPosition: Long = 0
)
