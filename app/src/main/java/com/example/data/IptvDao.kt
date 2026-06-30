package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // --- Channels ---
    @Query("SELECT * FROM channels ORDER BY orderIndex ASC, name ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isVisible = 1 ORDER BY orderIndex ASC, name ASC")
    fun getVisibleChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChannels(channels: List<ChannelEntity>)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: String)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("UPDATE channels SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)

    // --- Live Matches ---
    @Query("SELECT * FROM live_matches ORDER BY matchTime ASC")
    fun getAllLiveMatches(): Flow<List<LiveMatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveMatch(match: LiveMatchEntity)

    @Delete
    suspend fun deleteLiveMatch(match: LiveMatchEntity)

    @Query("DELETE FROM live_matches WHERE id = :id")
    suspend fun deleteLiveMatchById(id: String)

    @Query("DELETE FROM live_matches")
    suspend fun clearLiveMatches()

    // --- Notices ---
    @Query("SELECT * FROM notices ORDER BY timestamp DESC")
    fun getAllNotices(): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Delete
    suspend fun deleteNotice(notice: NoticeEntity)

    @Query("DELETE FROM notices WHERE id = :id")
    suspend fun deleteNoticeById(id: String)

    // --- Ad Config ---
    @Query("SELECT * FROM ad_configs WHERE id = 'global_ads'")
    fun getAdConfigFlow(): Flow<AdConfigEntity?>

    @Query("SELECT * FROM ad_configs WHERE id = 'global_ads'")
    suspend fun getAdConfig(): AdConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdConfig(config: AdConfigEntity)

    // --- App Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 'global_settings'")
    fun getAppSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'global_settings'")
    suspend fun getAppSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettingsEntity)

    // --- Favorites ---
    @Query("SELECT * FROM favorites ORDER BY favoritedAt DESC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    fun isFavoriteFlow(channelId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavorite(channelId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun deleteFavorite(channelId: String)

    // --- Watch History ---
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getWatchHistoryFlow(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM watch_history WHERE channelId = :channelId")
    suspend fun deleteHistory(channelId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}
