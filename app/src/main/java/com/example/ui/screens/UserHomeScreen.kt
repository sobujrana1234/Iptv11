package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.IptvViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserHomeScreen(
    viewModel: IptvViewModel,
    onChannelSelected: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val liveMatches by viewModel.liveMatches.collectAsState()
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.watchHistory.collectAsState()

    var activeTab by remember { mutableStateOf("LIVE_TV") } // LIVE_TV, FAVORITES, SCHEDULE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF141414),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = activeTab == "LIVE_TV",
                    onClick = { activeTab = "LIVE_TV" },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "Live TV") },
                    label = { Text("Live TV") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "FAVORITES",
                    onClick = { activeTab = "FAVORITES" },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("My Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = Color(0xFF0F0F0F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Scrolling Marquee Notice (Always visible)
            val marqueeNotice = notices.firstOrNull { it.isMarquee }
            if (marqueeNotice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = marqueeNotice.message,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            // 2. Custom App Logo Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFFE50914))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SportsFootball,
                            contentDescription = "Logo icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEKBD IPTV",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                // Status Tracker Simulating Connections
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF222222), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF66))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00FF66),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoading) {
                // Skeletons rendering during initial loads
                SkeletonHomeScreen()
            } else {
                when (activeTab) {
                    "LIVE_TV" -> {
                        LiveTvContent(
                            viewModel = viewModel,
                            liveMatches = liveMatches,
                            notices = notices,
                            categories = categories,
                            channels = channels,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            favorites = favorites,
                            onChannelSelected = onChannelSelected
                        )
                    }
                    "FAVORITES" -> {
                        MyLibraryContent(
                            favorites = favorites,
                            history = history,
                            channels = channels,
                            onChannelSelected = onChannelSelected,
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveTvContent(
    viewModel: IptvViewModel,
    liveMatches: List<LiveMatchEntity>,
    notices: List<NoticeEntity>,
    categories: List<CategoryEntity>,
    channels: List<ChannelEntity>,
    searchQuery: String,
    selectedCategory: String,
    favorites: List<com.example.data.FavoriteEntity>,
    onChannelSelected: (ChannelEntity) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Match Alarm Banners
        val textNotices = notices.filter { !it.isMarquee && it.type == "BREAKING" }
        if (textNotices.isNotEmpty()) {
            textNotices.forEach { notice ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1012).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = "Alert",
                            tint = Color(0xFFE50914),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = notice.message,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Live Matches Panel
        if (liveMatches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔥 LIVE SPORTS COVERAGE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "HD FREE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(liveMatches) { match ->
                    LiveMatchCard(match = match, onPlay = {
                        val mockChannel = ChannelEntity(
                            id = match.id,
                            name = "${match.team1Name} vs ${match.team2Name} Live",
                            category = "Live Sports",
                            logoUrl = match.team1Logo,
                            streamUrl = match.streamUrl
                        )
                        onChannelSelected(mockChannel)
                    })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search your favorite channels...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_search_input"),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF333333)
                )
            )
        }

        // Category Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                CategoryTab(
                    name = "All Channels",
                    isSelected = selectedCategory == "All",
                    onClick = { viewModel.onCategorySelect("All") }
                )
            }
            items(categories) { cat ->
                CategoryTab(
                    name = cat.name,
                    isSelected = selectedCategory == cat.name,
                    onClick = { viewModel.onCategorySelect(cat.name) }
                )
            }
        }

        // Filtering Logic
        val filteredChannels = channels.filter { channel ->
            val matchesCategory = (selectedCategory == "All") || (channel.category == selectedCategory)
            val matchesSearch = channel.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TvOff,
                        contentDescription = "No channels",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No channels found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            // Infinite Grid for channels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1200.dp) // Bound the grid height
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val listHeight = (filteredChannels.size / 2 + 1) * 110
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false, // delegate scroll to the outer column parent
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listHeight.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isFavorited = favorites.any { it.channelId == channel.id }
                        ChannelGridCard(
                            channel = channel,
                            isFavorite = isFavorited,
                            onPlayClick = { onChannelSelected(channel) },
                            onFavClick = { viewModel.toggleFavorite(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyLibraryContent(
    favorites: List<com.example.data.FavoriteEntity>,
    history: List<com.example.data.HistoryEntity>,
    channels: List<ChannelEntity>,
    onChannelSelected: (ChannelEntity) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    val favoriteChannels = channels.filter { ch -> favorites.any { fav -> fav.channelId == ch.id } }
    val historyChannels = channels.filter { ch -> history.any { hist -> hist.channelId == ch.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Favorites Row
        Text(
            "💖 MY FAVORITE CHANNELS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (favoriteChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Your favorites folder is empty.", color = Color.Gray)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favoriteChannels) { ch ->
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .clickable { onChannelSelected(ch) }
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = ch.logoUrl,
                                contentDescription = ch.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ch.name,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue Watching Row
        Text(
            "⏳ RECENTLY WATCHED",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (historyChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No watch history recorded yet.", color = Color.Gray)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                historyChannels.take(5).forEach { ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                            .clickable { onChannelSelected(ch) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ch.logoUrl,
                                contentDescription = ch.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(ch.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(ch.category, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveMatchCard(match: LiveMatchEntity, onPlay: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onPlay() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF262626), Color(0xFF151515))
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (match.status) {
                                    "LIVE" -> Color(0xFFE50914)
                                    "UPCOMING" -> Color(0xFF00AAFF)
                                    else -> Color.DarkGray
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = match.status,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "CRICKET T20",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Teams logo matchup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = match.team1Logo,
                            contentDescription = match.team1Name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(match.team1Name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Text("VS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Red)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = match.team2Logo,
                            contentDescription = match.team2Name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(match.team2Name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tap to Stream Free",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryTab(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ChannelGridCard(
    channel: ChannelEntity,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onPlayClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.category,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onFavClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Fav",
                    tint = if (isFavorite) Color.Red else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SkeletonHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF222222))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1A1A1A))
                )
            }
        }
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                )
            }
        }
    }
}
