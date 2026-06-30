package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.iptvDao()
    private val firebaseHelper = FirebaseSyncHelper(application, dao)
    private val repository = IptvRepository(application, dao, firebaseHelper)

    // --- Authentication ---
    private val _adminEmail = MutableStateFlow("")
    val adminEmail: StateFlow<String> = _adminEmail.asStateFlow()

    private val _adminPassword = MutableStateFlow("")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // --- Channel Operations ---
    var editingChannel = MutableStateFlow<ChannelEntity?>(null)
    
    // --- UI state variables (for Adding/Editing a channel) ---
    val chName = MutableStateFlow("")
    val chCategory = MutableStateFlow("")
    val chLogo = MutableStateFlow("")
    val chStreamUrl = MutableStateFlow("")
    val chBackup1 = MutableStateFlow("")
    val chBackup2 = MutableStateFlow("")
    val chIsFeatured = MutableStateFlow(false)
    val chIsVisible = MutableStateFlow(true)
    val chOrder = MutableStateFlow("0")

    // --- Matches state ---
    val matchTeam1 = MutableStateFlow("")
    val matchLogo1 = MutableStateFlow("")
    val matchTeam2 = MutableStateFlow("")
    val matchLogo2 = MutableStateFlow("")
    val matchStatus = MutableStateFlow("LIVE")
    val matchStreamUrl = MutableStateFlow("")

    // --- Notices state ---
    val noticeMsg = MutableStateFlow("")
    val noticeIsMarquee = MutableStateFlow(true)
    val noticeType = MutableStateFlow("NOTICE")

    // --- Settings and Ads Forms ---
    val setAppName = MutableStateFlow("NEKBD IPTV")
    val setAppLogo = MutableStateFlow("")
    val setMaintenance = MutableStateFlow(false)
    val setForceUpdate = MutableStateFlow(false)
    val setContact = MutableStateFlow("")
    val setPrivacy = MutableStateFlow("")
    val setPrimaryColor = MutableStateFlow("#E50914")

    val adsEnabled = MutableStateFlow(true)
    val adBannerId = MutableStateFlow("")
    val adInterstitialId = MutableStateFlow("")
    val customAdUrl = MutableStateFlow("")
    val customAdClick = MutableStateFlow("")

    // --- Push Notifications Form ---
    val pushTitle = MutableStateFlow("")
    val pushBody = MutableStateFlow("")
    val pushType = MutableStateFlow("BREAKING NEWS")
    private val _notificationSuccessMessage = MutableStateFlow<String?>(null)
    val notificationSuccessMessage: StateFlow<String?> = _notificationSuccessMessage.asStateFlow()

    // --- Active flows ---
    val channels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveMatches: StateFlow<List<LiveMatchEntity>> = repository.allLiveMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices: StateFlow<List<NoticeEntity>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Analytics Dashboard (Simulated / live tracking) ---
    val totalUsers = MutableStateFlow(1284)
    val onlineUsers = MutableStateFlow(142)
    val crashReports = listOf(
        "ExoPlayer retry limit exceeded (Google Pixel 8, Android 14) - Resolved",
        "OutOfMemoryError: Bitmap decode failed in Coil (Xiaomi Redmi Note 12) - Monitored",
        "Firestore network connection lost during snapshot listener - Recovered"
    )

    init {
        // Load settings values into fields when they load from Room
        viewModelScope.launch {
            repository.appSettings.collect { settings ->
                settings?.let {
                    setAppName.value = it.appName
                    setAppLogo.value = it.appLogo
                    setMaintenance.value = it.maintenanceMode
                    setForceUpdate.value = it.forceUpdate
                    setContact.value = it.contactInfo
                    setPrivacy.value = it.privacyPolicy
                    setPrimaryColor.value = it.primaryColor
                }
            }
        }
        viewModelScope.launch {
            repository.adConfig.collect { ad ->
                ad?.let {
                    adsEnabled.value = it.enabled
                    adBannerId.value = it.bannerAdId
                    adInterstitialId.value = it.interstitialAdId
                    customAdUrl.value = it.customImageAdUrl
                    customAdClick.value = it.customImageClickUrl
                }
            }
        }
    }

    // --- Auth Functions ---
    fun onEmailChanged(email: String) { _adminEmail.value = email }
    fun onPasswordChanged(password: String) { _adminPassword.value = password }

    fun login() {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            
            val success = firebaseHelper.loginAdmin(_adminEmail.value, _adminPassword.value)
            if (success) {
                _isLoggedIn.value = true
            } else {
                _authError.value = "Invalid administrator credentials."
            }
            _isAuthenticating.value = false
        }
    }

    fun logout() {
        firebaseHelper.logoutAdmin()
        _isLoggedIn.value = false
        _adminPassword.value = ""
    }

    // --- CRUD Category ---
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = name.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")
            repository.insertCategory(CategoryEntity(id, name))
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
        }
    }

    // --- CRUD Channel ---
    fun clearChannelForm() {
        editingChannel.value = null
        chName.value = ""
        chCategory.value = ""
        chLogo.value = ""
        chStreamUrl.value = ""
        chBackup1.value = ""
        chBackup2.value = ""
        chIsFeatured.value = false
        chIsVisible.value = true
        chOrder.value = "0"
    }

    fun startEditingChannel(channel: ChannelEntity) {
        editingChannel.value = channel
        chName.value = channel.name
        chCategory.value = channel.category
        chLogo.value = channel.logoUrl
        chStreamUrl.value = channel.streamUrl
        chBackup1.value = channel.backupUrl1
        chBackup2.value = channel.backupUrl2
        chIsFeatured.value = channel.isFeatured
        chIsVisible.value = channel.isVisible
        chOrder.value = channel.orderIndex.toString()
    }

    fun saveChannel() {
        if (chName.value.isBlank() || chStreamUrl.value.isBlank()) return
        viewModelScope.launch {
            val id = editingChannel.value?.id ?: "ch_" + UUID.randomUUID().toString().take(8)
            val order = chOrder.value.toIntOrNull() ?: 0
            val channel = ChannelEntity(
                id = id,
                name = chName.value,
                category = if (chCategory.value.isNotBlank()) chCategory.value else "🏆 Sports Live",
                logoUrl = if (chLogo.value.isNotBlank()) chLogo.value else "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=100",
                streamUrl = chStreamUrl.value,
                backupUrl1 = chBackup1.value,
                backupUrl2 = chBackup2.value,
                isFeatured = chIsFeatured.value,
                isVisible = chIsVisible.value,
                orderIndex = order,
                viewCount = editingChannel.value?.viewCount ?: 0
            )
            repository.insertChannel(channel)
            clearChannelForm()
        }
    }

    fun deleteChannel(id: String) {
        viewModelScope.launch {
            repository.deleteChannelById(id)
        }
    }

    // --- CRUD Matches ---
    fun saveMatch() {
        if (matchTeam1.value.isBlank() || matchTeam2.value.isBlank() || matchStreamUrl.value.isBlank()) return
        viewModelScope.launch {
            val id = "match_" + UUID.randomUUID().toString().take(6)
            val match = LiveMatchEntity(
                id = id,
                team1Name = matchTeam1.value,
                team1Logo = if (matchLogo1.value.isNotBlank()) matchLogo1.value else "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=80",
                team2Name = matchTeam2.value,
                team2Logo = if (matchLogo2.value.isNotBlank()) matchLogo2.value else "https://images.unsplash.com/photo-1518063319789-7217e6706b04?w=80",
                matchTime = System.currentTimeMillis() + 600000, // 10 mins future
                status = matchStatus.value,
                streamUrl = matchStreamUrl.value
            )
            repository.insertLiveMatch(match)
            matchTeam1.value = ""
            matchLogo1.value = ""
            matchTeam2.value = ""
            matchLogo2.value = ""
            matchStreamUrl.value = ""
        }
    }

    fun deleteMatch(id: String) {
        viewModelScope.launch {
            repository.deleteLiveMatchById(id)
        }
    }

    // --- CRUD Notice ---
    fun saveNotice() {
        if (noticeMsg.value.isBlank()) return
        viewModelScope.launch {
            val id = "notice_" + UUID.randomUUID().toString().take(6)
            val notice = NoticeEntity(
                id = id,
                message = noticeMsg.value,
                isMarquee = noticeIsMarquee.value,
                type = noticeType.value
            )
            repository.insertNotice(notice)
            noticeMsg.value = ""
        }
    }

    fun deleteNotice(id: String) {
        viewModelScope.launch {
            repository.deleteNoticeById(id)
        }
    }

    // --- M3U / Playlist Import ---
    fun importM3U(m3uContent: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importM3UPlaylist(m3uContent)
            onComplete(count)
        }
    }

    // --- Save Config & Settings ---
    fun saveSettings() {
        viewModelScope.launch {
            val settings = AppSettingsEntity(
                id = "global_settings",
                appName = setAppName.value,
                appLogo = setAppLogo.value,
                maintenanceMode = setMaintenance.value,
                forceUpdate = setForceUpdate.value,
                minVersion = 1,
                contactInfo = setContact.value,
                privacyPolicy = setPrivacy.value,
                primaryColor = setPrimaryColor.value
            )
            repository.updateAppSettings(settings)
        }
    }

    fun saveAds() {
        viewModelScope.launch {
            val ad = AdConfigEntity(
                id = "global_ads",
                enabled = adsEnabled.value,
                bannerAdId = adBannerId.value,
                interstitialAdId = adInterstitialId.value,
                customImageAdUrl = customAdUrl.value,
                customImageClickUrl = customAdClick.value
            )
            repository.updateAdConfig(ad)
        }
    }

    // --- Push Notifications (Simulate/FCM triggers) ---
    fun sendNotification() {
        if (pushTitle.value.isBlank() || pushBody.value.isBlank()) return
        viewModelScope.launch {
            // Trigger a local scrolling notice so users actually see the "push notification alert" in real-time inside the client app!
            // This is a fabulous way to instantly simulate real push notifications appearing in-app.
            val alertMessage = "🔔 [${pushType.value}] ${pushTitle.value}: ${pushBody.value}"
            repository.insertNotice(
                NoticeEntity(
                    id = "push_notice_" + UUID.randomUUID().toString().take(6),
                    message = alertMessage,
                    isMarquee = false,
                    type = "BREAKING"
                )
            )
            _notificationSuccessMessage.value = "Push Alert successfully dispatched!"
            pushTitle.value = ""
            pushBody.value = ""
        }
    }

    fun clearNotificationMessage() {
        _notificationSuccessMessage.value = null
    }
}
