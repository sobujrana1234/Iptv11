package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class IptvRepository(
    private val context: Context,
    private val dao: IptvDao,
    private val firebaseSyncHelper: FirebaseSyncHelper
) {
    private val TAG = "IptvRepository"

    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allChannels: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val visibleChannels: Flow<List<ChannelEntity>> = dao.getVisibleChannels()
    val allLiveMatches: Flow<List<LiveMatchEntity>> = dao.getAllLiveMatches()
    val allNotices: Flow<List<NoticeEntity>> = dao.getAllNotices()
    val adConfig: Flow<AdConfigEntity?> = dao.getAdConfigFlow()
    val appSettings: Flow<AppSettingsEntity?> = dao.getAppSettingsFlow()
    val favorites: Flow<List<FavoriteEntity>> = dao.getAllFavoritesFlow()
    val watchHistory: Flow<List<HistoryEntity>> = dao.getWatchHistoryFlow()

    fun isFavorite(channelId: String): Flow<Boolean> = dao.isFavoriteFlow(channelId)

    // --- Prepopulate Defaults ---
    suspend fun prepopulateDatabaseIfEmpty() {
        val existingChannels = dao.getAllChannels().first()
        if (existingChannels.isNotEmpty()) {
            Log.d(TAG, "Database is already populated with ${existingChannels.size} channels.")
            return
        }

        Log.d(TAG, "Pre-populating database with premium sports and news categories, channels, and live matches.")

        // 1. Add default categories
        val defaultCategories = listOf(
            CategoryEntity("sports", "🏆 Sports Live"),
            CategoryEntity("news", "📰 News 24/7"),
            CategoryEntity("entertainment", "🎬 Entertainment"),
            CategoryEntity("music", "🎵 Music & Lifestyle"),
            CategoryEntity("movies", "🍿 Movies")
        )
        for (cat in defaultCategories) {
            dao.insertCategory(cat)
            firebaseSyncHelper.syncCategoriesToFirebase(listOf(cat))
        }

        // 2. Add default channels with legal, stable, actual HLS stream links (e.g. standard open-source testing streams or legal free-to-air channels)
        // These can play successfully in ExoPlayer so they are fully functional!
        val defaultChannels = listOf(
            ChannelEntity(
                id = "ch_nekbd_sports",
                name = "NEKBD Sports HD",
                category = "🏆 Sports Live",
                logoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=120&auto=format&fit=crop&q=60",
                streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8", // bipbop HLS test
                backupUrl1 = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                isFeatured = true,
                orderIndex = 1
            ),
            ChannelEntity(
                id = "ch_espn_live",
                name = "ESPN Live HD",
                category = "🏆 Sports Live",
                logoUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=120&auto=format&fit=crop&q=60",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", // tears of steel test
                backupUrl1 = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8",
                isFeatured = true,
                orderIndex = 2
            ),
            ChannelEntity(
                id = "ch_sky_sports",
                name = "Sky Sports Football",
                category = "🏆 Sports Live",
                logoUrl = "https://images.unsplash.com/photo-1518063319789-7217e6706b04?w=120&auto=format&fit=crop&q=60",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", // Akamai master test
                isFeatured = false,
                orderIndex = 3
            ),
            ChannelEntity(
                id = "ch_cnn_news",
                name = "NEKBD News 24",
                category = "📰 News 24/7",
                logoUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=120&auto=format&fit=crop&q=60",
                streamUrl = "http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8", // skate test
                isFeatured = false,
                orderIndex = 4
            ),
            ChannelEntity(
                id = "ch_bbc_world",
                name = "BBC World Live",
                category = "📰 News 24/7",
                logoUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=120&auto=format&fit=crop&q=60",
                streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8",
                isFeatured = true,
                orderIndex = 5
            ),
            ChannelEntity(
                id = "ch_hbo_movies",
                name = "HBO Movies HD",
                category = "🍿 Movies",
                logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=120&auto=format&fit=crop&q=60",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                isFeatured = false,
                orderIndex = 6
            ),
            ChannelEntity(
                id = "ch_mtv_music",
                name = "MTV Music Global",
                category = "🎵 Music & Lifestyle",
                logoUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=120&auto=format&fit=crop&q=60",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                isFeatured = false,
                orderIndex = 7
            )
        )

        for (channel in defaultChannels) {
            dao.insertChannel(channel)
        }
        firebaseSyncHelper.syncChannelsToFirebase(defaultChannels)

        // 3. Add default live matches with countdown badges
        val currentTime = System.currentTimeMillis()
        val defaultMatches = listOf(
            LiveMatchEntity(
                id = "match_1",
                team1Name = "Argentina",
                team1Logo = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=80&auto=format&fit=crop&q=60",
                team2Name = "Brazil",
                team2Logo = "https://images.unsplash.com/photo-1518063319789-7217e6706b04?w=80&auto=format&fit=crop&q=60",
                matchTime = currentTime + 300000, // 5 minutes in future (LIVE soon!)
                status = "LIVE",
                streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"
            ),
            LiveMatchEntity(
                id = "match_2",
                team1Name = "Real Madrid",
                team1Logo = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=80&auto=format&fit=crop&q=60",
                team2Name = "Barcelona",
                team2Logo = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=80&auto=format&fit=crop&q=60",
                matchTime = currentTime + 86400000, // tomorrow (UPCOMING)
                status = "UPCOMING",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
            ),
            LiveMatchEntity(
                id = "match_3",
                team1Name = "Bangladesh",
                team1Logo = "https://images.unsplash.com/photo-1540747737956-37872176751e?w=80&auto=format&fit=crop&q=60",
                team2Name = "India",
                team2Logo = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=80&auto=format&fit=crop&q=60",
                matchTime = currentTime - 7200000, // 2 hours ago (FINISHED)
                status = "FINISHED",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
            )
        )
        for (match in defaultMatches) {
            dao.insertLiveMatch(match)
        }

        // 4. Add default scrolling notices
        val defaultNotices = listOf(
            NoticeEntity(
                id = "notice_marquee",
                message = "🔥 Welcome to NEKBD IPTV! Experience lag-free streaming for all major sports events, news channels, and live football. Join our Telegram channel for daily schedules and support! 🔥",
                isMarquee = true,
                type = "NOTICE"
            ),
            NoticeEntity(
                id = "notice_breaking",
                message = "BREAKING: Argentina vs Brazil Live Stream starting now. Tap 'LIVE' banner above to watch!",
                isMarquee = false,
                type = "BREAKING"
            )
        )
        for (notice in defaultNotices) {
            dao.insertNotice(notice)
        }

        // 5. Add default ads config
        dao.insertAdConfig(AdConfigEntity())

        // 6. Add default settings
        dao.insertAppSettings(AppSettingsEntity())
    }

    // --- Channel CRUD Operations (Admin) ---
    suspend fun insertChannel(channel: ChannelEntity) {
        dao.insertChannel(channel)
        if (firebaseSyncHelper.isFirebaseReady()) {
            firebaseSyncHelper.syncChannelsToFirebase(listOf(channel))
        }
    }

    suspend fun deleteChannel(channel: ChannelEntity) {
        dao.deleteChannel(channel)
        firebaseSyncHelper.deleteChannelFromFirebase(channel.id)
    }

    suspend fun deleteChannelById(id: String) {
        dao.deleteChannelById(id)
        firebaseSyncHelper.deleteChannelFromFirebase(id)
    }

    suspend fun incrementViewCount(id: String) {
        dao.incrementViewCount(id)
    }

    // --- Category CRUD Operations (Admin) ---
    suspend fun insertCategory(category: CategoryEntity) {
        dao.insertCategory(category)
        if (firebaseSyncHelper.isFirebaseReady()) {
            firebaseSyncHelper.syncCategoriesToFirebase(listOf(category))
        }
    }

    suspend fun deleteCategoryById(id: String) {
        dao.deleteCategoryById(id)
        firebaseSyncHelper.deleteCategoryFromFirebase(id)
    }

    // --- Match Operations ---
    suspend fun insertLiveMatch(match: LiveMatchEntity) {
        dao.insertLiveMatch(match)
    }

    suspend fun deleteLiveMatchById(id: String) {
        dao.deleteLiveMatchById(id)
    }

    // --- Notice Operations ---
    suspend fun insertNotice(notice: NoticeEntity) {
        dao.insertNotice(notice)
    }

    suspend fun deleteNoticeById(id: String) {
        dao.deleteNoticeById(id)
    }

    // --- Favorites ---
    suspend fun toggleFavorite(channelId: String) {
        val isFav = dao.isFavorite(channelId)
        if (isFav) {
            dao.deleteFavorite(channelId)
        } else {
            dao.insertFavorite(FavoriteEntity(channelId))
        }
    }

    // --- History ---
    suspend fun addToHistory(channelId: String, position: Long = 0) {
        dao.insertHistory(HistoryEntity(channelId, System.currentTimeMillis(), position))
    }

    // --- Ad Config ---
    suspend fun updateAdConfig(config: AdConfigEntity) {
        dao.insertAdConfig(config)
    }

    // --- App Settings ---
    suspend fun updateAppSettings(settings: AppSettingsEntity) {
        dao.insertAppSettings(settings)
    }

    // --- M3U / Playlist Parser ---
    suspend fun importM3UPlaylist(m3uContent: String): Int {
        var count = 0
        try {
            val lines = m3uContent.lineSequence().map { it.trim() }.toList()
            var currentName = ""
            var currentLogo = ""
            var currentGroup = "🥇 Imported Channels"

            // Ensure category exists
            dao.insertCategory(CategoryEntity("imported_channels", "🥇 Imported Channels"))

            for (i in lines.indices) {
                val line = lines[i]
                if (line.startsWith("#EXTINF:")) {
                    // Extract tvg-logo
                    val logoRegex = """tvg-logo="([^"]+)"""".toRegex()
                    val logoMatch = logoRegex.find(line)
                    currentLogo = logoMatch?.groupValues?.get(1) ?: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=100"

                    // Extract group-title
                    val groupRegex = """group-title="([^"]+)"""".toRegex()
                    val groupMatch = groupRegex.find(line)
                    val groupName = groupMatch?.groupValues?.get(1) ?: "🥇 Imported Channels"
                    
                    val catId = groupName.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")
                    currentGroup = groupName
                    dao.insertCategory(CategoryEntity(catId, groupName))

                    // Extract name (after the last comma)
                    val lastCommaIndex = line.lastIndexOf(',')
                    currentName = if (lastCommaIndex != -1 && lastCommaIndex < line.length - 1) {
                        line.substring(lastCommaIndex + 1).trim()
                    } else {
                        "Imported Channel ${count + 1}"
                    }
                } else if (line.isNotEmpty() && !line.startsWith("#")) {
                    // This is the URL line
                    val streamUrl = line
                    if (currentName.isNotEmpty()) {
                        val id = "imported_" + UUID.randomUUID().toString().take(6)
                        val channel = ChannelEntity(
                            id = id,
                            name = currentName,
                            category = currentGroup,
                            logoUrl = currentLogo,
                            streamUrl = streamUrl,
                            orderIndex = count + 100
                        )
                        dao.insertChannel(channel)
                        count++
                    }
                    // Reset
                    currentName = ""
                    currentLogo = ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "M3U Import error", e)
        }
        return count
    }
}
