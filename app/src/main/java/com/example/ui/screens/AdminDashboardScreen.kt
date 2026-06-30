package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CategoryEntity
import com.example.data.ChannelEntity
import com.example.ui.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val liveMatches by viewModel.liveMatches.collectAsState()
    val notices by viewModel.notices.collectAsState()
    val totalUsersVal by viewModel.totalUsers.collectAsState()
    val onlineUsersVal by viewModel.onlineUsers.collectAsState()
    val notificationSuccessMessage by viewModel.notificationSuccessMessage.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Analytics", "Channels", "Playlists", "Home Control", "Settings")

    // Local dialog controllers
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var m3uInputContent by remember { mutableStateOf("") }

    // Floating success message tracker
    LaunchedEffect(notificationSuccessMessage) {
        notificationSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNotificationMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("IPTV CONTROLLER", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                        Text("NEKBD Dual System Backend", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141414),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab Menu
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (activeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> AnalyticsTab(totalUsersVal, onlineUsersVal, channels, viewModel.crashReports)
                    1 -> ChannelsManagementTab(
                        viewModel = viewModel,
                        channels = channels,
                        categories = categories,
                        onAddCategoryClick = { showAddCategoryDialog = true }
                    )
                    2 -> PlaylistsTab(
                        m3uContent = m3uInputContent,
                        onM3uContentChange = { m3uInputContent = it },
                        onImport = { content ->
                            viewModel.importM3U(content) { count ->
                                Toast.makeText(context, "Successfully imported $count channels!", Toast.LENGTH_LONG).show()
                                m3uInputContent = ""
                            }
                        }
                    )
                    3 -> HomeControlTab(
                        viewModel = viewModel,
                        liveMatches = liveMatches,
                        notices = notices
                    )
                    4 -> AppSettingsTab(viewModel)
                }
            }
        }

        // Add Category dialog
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Create New Category", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("e.g. Sports Live, Movies HD") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addCategory(newCategoryName)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }
    }
}

@Composable
fun AnalyticsTab(
    totalUsers: Int,
    onlineUsers: Int,
    channels: List<ChannelEntity>,
    crashes: List<String>
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.People, contentDescription = "Users", tint = Color(0xFF00FF88))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Total Users", color = Color.Gray, fontSize = 12.sp)
                    Text("$totalUsers", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Group, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Online Users", color = Color.Gray, fontSize = 12.sp)
                    Text("$onlineUsers", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Most Watched Channels List
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Most Watched Channels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val topChannels = channels.sortedByDescending { it.viewCount }.take(5)
                if (topChannels.isEmpty()) {
                    Text("No channel view tracking data available yet.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    topChannels.forEachIndexed { index, ch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222222)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("#${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ch.name, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Text("${ch.viewCount} Views", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Ad Performance Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Ad Server Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Banner Ad Requests", color = Color.Gray, fontSize = 11.sp)
                        Text("14,281", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column {
                        Text("Interstitial CTR", color = Color.Gray, fontSize = 11.sp)
                        Text("4.85%", color = Color(0xFF00FF88), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column {
                        Text("Reward Payouts", color = Color.Gray, fontSize = 11.sp)
                        Text("432", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Live Crash Reports Log panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1415)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF551111))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = "Crashes", tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Recent System Logs & Crash Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                crashes.forEach { crash ->
                    Text(
                        "⚠️ $crash",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelsManagementTab(
    viewModel: AdminViewModel,
    channels: List<ChannelEntity>,
    categories: List<CategoryEntity>,
    onAddCategoryClick: () -> Unit
) {
    var isFormVisible by remember { mutableStateOf(false) }

    // Channel Form states
    val name by viewModel.chName.collectAsState()
    val category by viewModel.chCategory.collectAsState()
    val logo by viewModel.chLogo.collectAsState()
    val streamUrl by viewModel.chStreamUrl.collectAsState()
    val backup1 by viewModel.chBackup1.collectAsState()
    val backup2 by viewModel.chBackup2.collectAsState()
    val isFeatured by viewModel.chIsFeatured.collectAsState()
    val isVisible by viewModel.chIsVisible.collectAsState()
    val order by viewModel.chOrder.collectAsState()
    val editingChannel by viewModel.editingChannel.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Categories row controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Channel Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddCategoryClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Cat", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Category", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.clearChannelForm()
                            isFormVisible = !isFormVisible
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            if (isFormVisible) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Toggle Form"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFormVisible) "Close Form" else "Add Channel", fontSize = 11.sp)
                    }
                }
            }
        }

        // Add/Edit Channel form panel
        if (isFormVisible || editingChannel != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (editingChannel != null) "Edit Channel" else "New Channel Specifications",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.chName.value = it },
                            label = { Text("Channel Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_channel_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Category Dropdown simulated
                        OutlinedTextField(
                            value = category,
                            onValueChange = { viewModel.chCategory.value = it },
                            label = { Text("Category Name (Matches existing category)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Sports Live, Movies HD") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = logo,
                            onValueChange = { viewModel.chLogo.value = it },
                            label = { Text("Logo Image URL (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = streamUrl,
                            onValueChange = { viewModel.chStreamUrl.value = it },
                            label = { Text("Primary M3U8/HLS URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_channel_stream_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = backup1,
                            onValueChange = { viewModel.chBackup1.value = it },
                            label = { Text("Backup stream URL 1 (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = backup2,
                            onValueChange = { viewModel.chBackup2.value = it },
                            label = { Text("Backup stream URL 2 (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = order,
                            onValueChange = { viewModel.chOrder.value = it },
                            label = { Text("Order Index (Sorting weight)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Checkboxes
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isFeatured,
                                onCheckedChange = { viewModel.chIsFeatured.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Feature on Home Banner", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isVisible,
                                onCheckedChange = { viewModel.chIsVisible.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Make visible in client app", color = Color.White)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.saveChannel()
                                    isFormVisible = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_channel_save_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Save Channel")
                            }

                            Button(
                                onClick = {
                                    viewModel.clearChannelForm()
                                    isFormVisible = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }

        // Active Channel list
        items(channels) { channel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.width(160.dp)) {
                        Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(channel.category, color = Color.Gray, fontSize = 11.sp)
                        if (channel.isFeatured) {
                            Text("⭐ FEATURED", color = Color(0xFFFFCC00), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row {
                    IconButton(onClick = { viewModel.startEditingChannel(channel) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                    }
                    IconButton(onClick = { viewModel.deleteChannel(channel.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    m3uContent: String,
    onM3uContentChange: (String) -> Unit,
    onImport: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Bulk M3U Playlist Parser",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            "Paste complete M3U playlist file lines here. It automatically extracts stream feeds, matching categories, and names to insert instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        OutlinedTextField(
            value = m3uContent,
            onValueChange = onM3uContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .testTag("admin_m3u_input"),
            placeholder = { Text("#EXTM3U\n#EXTINF:-1 tvg-logo=\"https://img.com\" group-title=\"Sports\",ESPN\nhttp://stream.m3u8", color = Color.DarkGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            maxLines = 100
        )

        Button(
            onClick = { onImport(m3uContent) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("admin_import_m3u_submit"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = m3uContent.isNotBlank()
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Upload")
            Spacer(modifier = Modifier.width(8.dp))
            Text("PARSE & IMPORT PLAYLIST")
        }
    }
}

@Composable
fun HomeControlTab(
    viewModel: AdminViewModel,
    liveMatches: List<com.example.data.LiveMatchEntity>,
    notices: List<com.example.data.NoticeEntity>
) {
    val scrollState = rememberScrollState()

    // Match forms
    val mTeam1 by viewModel.matchTeam1.collectAsState()
    val mLogo1 by viewModel.matchLogo1.collectAsState()
    val mTeam2 by viewModel.matchTeam2.collectAsState()
    val mLogo2 by viewModel.matchLogo2.collectAsState()
    val mStatus by viewModel.matchStatus.collectAsState()
    val mStream by viewModel.matchStreamUrl.collectAsState()

    // Notice forms
    val nMsg by viewModel.noticeMsg.collectAsState()
    val nMarquee by viewModel.noticeIsMarquee.collectAsState()
    val nType by viewModel.noticeType.collectAsState()

    // Notification form
    val pTitle by viewModel.pushTitle.collectAsState()
    val pBody by viewModel.pushBody.collectAsState()
    val pType by viewModel.pushType.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live matches section
        Text("Sports & Live Match Banners", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Upcoming/Live Match", color = Color.White, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mTeam1,
                        onValueChange = { viewModel.matchTeam1.value = it },
                        label = { Text("Team 1 Name") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    OutlinedTextField(
                        value = mTeam2,
                        onValueChange = { viewModel.matchTeam2.value = it },
                        label = { Text("Team 2 Name") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mLogo1,
                        onValueChange = { viewModel.matchLogo1.value = it },
                        label = { Text("Team 1 Logo URL") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    OutlinedTextField(
                        value = mLogo2,
                        onValueChange = { viewModel.matchLogo2.value = it },
                        label = { Text("Team 2 Logo URL") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                OutlinedTextField(
                    value = mStream,
                    onValueChange = { viewModel.matchStreamUrl.value = it },
                    label = { Text("HLS Match Streaming URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Button(
                    onClick = { viewModel.saveMatch() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Sports Match Feed")
                }
            }
        }

        // Active Matches list
        liveMatches.forEach { match ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${match.team1Name} vs ${match.team2Name} (${match.status})", color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.deleteMatch(match.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }

        Divider(color = Color.DarkGray)

        // Marquees notices section
        Text("Ticker Marquee & Notices Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nMsg,
                    onValueChange = { viewModel.noticeMsg.value = it },
                    label = { Text("Message Announcement") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = nMarquee,
                        onCheckedChange = { viewModel.noticeIsMarquee.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Display as scrolling marquee", color = Color.White)
                }

                Button(
                    onClick = { viewModel.saveNotice() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Notice / Ticker Tape")
                }
            }
        }

        notices.forEach { notice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(notice.message, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.deleteNotice(notice.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }

        Divider(color = Color.DarkGray)

        // Push Alert Simulator (Critical for Dual App controls!)
        Text("Trigger Push & Match Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1C12)),
            border = BorderStroke(1.dp, Color(0xFFC0A040))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dispatches FCM Push Broadcast Alerts", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = pTitle,
                    onValueChange = { viewModel.pushTitle.value = it },
                    label = { Text("Notification Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFCC00))
                )

                OutlinedTextField(
                    value = pBody,
                    onValueChange = { viewModel.pushBody.value = it },
                    label = { Text("Notification Body Message") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFCC00))
                )

                Button(
                    onClick = { viewModel.sendNotification() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                    enabled = pTitle.isNotBlank() && pBody.isNotBlank()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Alert", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DISPATCH FCM BROADCAST", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AppSettingsTab(viewModel: AdminViewModel) {
    val scrollState = rememberScrollState()

    val name by viewModel.setAppName.collectAsState()
    val logo by viewModel.setAppLogo.collectAsState()
    val maint by viewModel.setMaintenance.collectAsState()
    val force by viewModel.setForceUpdate.collectAsState()
    val contact by viewModel.setContact.collectAsState()
    val privacy by viewModel.setPrivacy.collectAsState()

    val adsActive by viewModel.adsEnabled.collectAsState()
    val adBanner by viewModel.adBannerId.collectAsState()
    val customAdUrl by viewModel.customAdUrl.collectAsState()
    val customAdClick by viewModel.customAdClick.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Customization Section
        Text("Application Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.setAppName.value = it },
                    label = { Text("Launcher Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = logo,
                    onValueChange = { viewModel.setAppLogo.value = it },
                    label = { Text("Splash Art Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { viewModel.setContact.value = it },
                    label = { Text("Developer Contact Support") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = privacy,
                    onValueChange = { viewModel.setPrivacy.value = it },
                    label = { Text("Privacy Policy URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = maint,
                        onCheckedChange = { viewModel.setMaintenance.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Enable Maintenance Lock (Server Shutdown)", color = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = force,
                        onCheckedChange = { viewModel.setForceUpdate.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Trigger Force Update Banner", color = Color.White)
                }

                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Update App Config Settings")
                }
            }
        }

        // Ads settings Section
        Text("Monetization Server Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = adsActive,
                        onCheckedChange = { viewModel.adsEnabled.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Inject Google Admob & Custom Ads", color = Color.White)
                }

                OutlinedTextField(
                    value = adBanner,
                    onValueChange = { viewModel.adBannerId.value = it },
                    label = { Text("Admob Banner ID Unit") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = customAdUrl,
                    onValueChange = { viewModel.customAdUrl.value = it },
                    label = { Text("Custom image Ad Image Banner URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = customAdClick,
                    onValueChange = { viewModel.customAdClick.value = it },
                    label = { Text("Custom Image Ad Redirection Target Link") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Button(
                    onClick = { viewModel.saveAds() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply Monetization Setup")
                }
            }
        }
    }
}
