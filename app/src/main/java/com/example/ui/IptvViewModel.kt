package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.iptvDao()
    private val firebaseSyncHelper = FirebaseSyncHelper(application, dao)
    private val repository = IptvRepository(application, dao, firebaseSyncHelper)

    // --- Loading States ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Filter States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // --- Active Streaming State ---
    private val _currentPlayingChannel = MutableStateFlow<ChannelEntity?>(null)
    val currentPlayingChannel: StateFlow<ChannelEntity?> = _currentPlayingChannel.asStateFlow()

    // --- Core Flows ---
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> = repository.visibleChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveMatches: StateFlow<List<LiveMatchEntity>> = repository.allLiveMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices: StateFlow<List<NoticeEntity>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adConfig: StateFlow<AdConfigEntity?> = repository.adConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val favorites: StateFlow<List<FavoriteEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<HistoryEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _isLoading.value = true
            // Prepopulate if database is empty
            repository.prepopulateDatabaseIfEmpty()
            
            // Connect snapshot listeners if Firebase is configured
            firebaseSyncHelper.setupRealtimeListeners(
                onChannelsUpdated = { list ->
                    viewModelScope.launch { dao.insertAllChannels(list) }
                },
                onCategoriesUpdated = { list ->
                    viewModelScope.launch { for (c in list) dao.insertCategory(c) }
                },
                onMatchesUpdated = { list ->
                    viewModelScope.launch { for (m in list) dao.insertLiveMatch(m) }
                },
                onNoticesUpdated = { list ->
                    viewModelScope.launch { for (n in list) dao.insertNotice(n) }
                },
                onAdConfigUpdated = { config ->
                    viewModelScope.launch { dao.insertAdConfig(config) }
                },
                onAppSettingsUpdated = { settings ->
                    viewModelScope.launch { dao.insertAppSettings(settings) }
                }
            )
            
            delay(1200) // Simulating smooth initial skeleton loading transitions
            _isLoading.value = false
        }
    }

    // --- Search & Filters ---
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    // --- Channel Interactivity ---
    fun selectChannel(channel: ChannelEntity) {
        _currentPlayingChannel.value = channel
        viewModelScope.launch {
            repository.incrementViewCount(channel.id)
            repository.addToHistory(channel.id)
        }
    }

    fun stopPlaying() {
        _currentPlayingChannel.value = null
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId)
        }
    }

    // --- Pull To Refresh ---
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1500) // Simulated network/cache refresh sync
            _isRefreshing.value = false
        }
    }
}
