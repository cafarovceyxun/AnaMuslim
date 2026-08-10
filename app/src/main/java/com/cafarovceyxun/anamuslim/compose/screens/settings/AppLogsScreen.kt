package com.cafarovceyxun.anamuslim.compose.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.api.ApiConfig
import com.cafarovceyxun.anamuslim.components.appLogs.AppLogModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.extensions.copyToClipboard
import com.cafarovceyxun.anamuslim.utils.supabase.AppLog
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.univ.MessageUtils
import com.cafarovceyxun.anamuslim.utils.univ.formatted
import com.cafarovceyxun.anamuslim.utils.univ.toDate
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

@Composable
fun AppLogsScreen() {
    val authViewModel = viewModel<AuthViewModel>()
    val session by authViewModel.session.collectAsState()
    val isAuthenticated = session != null

    // Better labels for tabs
    val tabLabels = remember(isAuthenticated) {
        if (isAuthenticated) listOf("Local Crash", "Local Error", "Remote (Supabase)")
        else listOf("Crash Logs", "Suppressed Logs")
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabLabels.size },
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { AppBar(title = stringResource(R.string.appLogs), shadowElevation = 0.dp) },
    ) { paddingValues ->
        if (!isAuthenticated) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_keyhole_closed),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colorScheme.onSurface.alpha(0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Admin girişi tələb olunur",
                        style = typography.titleMedium,
                        color = colorScheme.onSurface.alpha(0.6f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Logları görmək üçün əvvəlcə giriş edin.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurface.alpha(0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabLabels.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title, style = typography.labelLarge) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) { page ->
                when (page) {
                    0 -> CrashLogs()
                    1 -> SuppressedLogs()
                    2 -> if (isAuthenticated) RemoteLogs()
                }
            }
        }
    }
}


@Composable
private fun CrashLogs() {
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<AppLogModel>>(emptyList()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val files = Log.CRASH_LOGS_DIR.listFiles()

        if (files.isNullOrEmpty()) {
            logs = emptyList()
        } else {
            logs = files
                .sortedByDescending { it.lastModified() }
                .map { logFile ->
                    val logText = logFile.readText()
                    val logShort = if (logText.length > 200) logText.substring(
                        0,
                        200
                    ) + "... ${logText.length - 200} more chars"
                    else logText

                    val parsedDate = logFile.name.toDate(Log.FILE_NAME_DATE_FORMAT)
                    val formattedDateTime = parsedDate?.formatted() ?: logFile.name

                    AppLogModel(
                        datetime = formattedDateTime,
                        place = "Fatal Crash",
                        file = logFile,
                        log = logText,
                        logShort = logShort,
                    )
                }
        }

        isLoading = false
    }

    if (isLoading) {
        return Loader(true)
    }

    if (logs.isEmpty()) {
        return Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.textNoLogsFound),
                textAlign = TextAlign.Center,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(logs.size) { index ->
            val log = logs[index]
            AppLogItem(
                log,
                isCrash = true,
            ) {
                it.file.delete()
                logs = logs
                    .toMutableList()
                    .apply { remove(it) }

                MessageUtils.showRemovableToast(
                    context,
                    R.string.logRemoved,
                    Toast.LENGTH_SHORT,
                )
            }
        }
    }
}

@Composable
private fun SuppressedLogs() {
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<AppLogModel>>(emptyList()) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val files = Log.SUPPRESSED_LOGS_DIR?.listFiles()

        if (files.isNullOrEmpty()) {
            logs = emptyList()
        } else {
            logs = files
                .sortedByDescending { it.lastModified() }
                .map { logFile ->
                    val (datetimeStr, location) = logFile.nameWithoutExtension.split("@")

                    val logText = logFile.readText()
                    val logShort = if (logText.length > 200) logText.substring(
                        0,
                        200
                    ) + "... ${logText.length - 200} more chars"
                    else logText

                    val parsedDate = datetimeStr.toDate(Log.FILE_NAME_DATE_FORMAT)
                    val formattedDateTime = parsedDate?.formatted() ?: logFile.name

                    AppLogModel(
                        datetime = formattedDateTime,
                        place = location,
                        file = logFile,
                        log = logText,
                        logShort = logShort,
                    )
                }
        }

        isLoading = false
    }

    if (isLoading) {
        return Loader(true)
    }

    if (logs.isEmpty()) {
        return Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.textNoLogsFound),
                textAlign = TextAlign.Center,
            )
        }
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(logs.size) { index ->
            val log = logs[index]
            AppLogItem(
                log,
                isCrash = false,
            ) {
                it.file.delete()
                logs = logs
                    .toMutableList()
                    .apply { remove(it) }

                MessageUtils.showRemovableToast(
                    context,
                    R.string.logRemoved,
                    Toast.LENGTH_SHORT,
                )
            }
        }
    }
}

@Composable
private fun RemoteLogs() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var logs by remember { mutableStateOf<List<AppLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchLogs() {
        scope.launch {
            isLoading = true
            errorMessage = null
            com.cafarovceyxun.anamuslim.utils.Log.d("SupabaseFetch", "Fetching logs from Supabase...")
            try {
                val result = SupabaseProvider.client.from("app_logs").select {
                    order("created_at", order = Order.DESCENDING)
                    limit(100)
                }
                logs = result.decodeList<AppLog>()
                com.cafarovceyxun.anamuslim.utils.Log.d("SupabaseFetch", "Successfully decoded ${logs.size} logs")
            } catch (e: Exception) {
                com.cafarovceyxun.anamuslim.utils.Log.d("SupabaseFetch", "Error fetching logs: ${e.message}")
                errorMessage = e.message ?: "Unknown error"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun clearRemoteLogs() {
        scope.launch {
            isLoading = true
            try {
                // In Supabase, to delete all (if policy allows), we usually filter by something always true
                SupabaseProvider.client.from("app_logs").delete {
                    filter {
                        neq("id", -1)
                    }
                }
                logs = emptyList()
                MessageUtils.showRemovableToast(context, "All remote logs cleared", Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                MessageUtils.showRemovableToast(context, "Clear failed: ${e.message}", Toast.LENGTH_LONG)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLogs()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && logs.isEmpty()) {
            Loader(true)
        } else if (errorMessage != null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.dr_icon_no_internet), null, modifier = Modifier.size(48.dp), tint = Color.Red.alpha(0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Error: $errorMessage", color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { fetchLogs() }) {
                        Text("Retry Fetch")
                    }
                }
            }
        } else if (!isLoading && logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No logs found in Supabase", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { fetchLogs() }) {
                        Text("Refresh")
                    }
                }
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: ${logs.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurface.alpha(0.6f)
                    )
                    
                    Row {
                        TextButton(onClick = { clearRemoteLogs() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                            Icon(painterResource(R.drawable.dr_icon_delete), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear All")
                        }
                        
                        TextButton(onClick = { fetchLogs() }) {
                            Icon(painterResource(R.drawable.dr_icon_refresh), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh")
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logs, key = { it.id ?: it.hashCode() }) { log ->
                        RemoteLogItem(log)
                    }
                }
            }
        }
        
        if (isLoading && logs.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun RemoteLogItem(log: AppLog) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isCrash = log.type == "crash"
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.alpha(0.3f)
        ),
        border = if (isCrash) BorderStroke(1.dp, Color.Red.alpha(0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = (if (isCrash) Color.Red else Color(0xFFFBC02D)).alpha(0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = log.type.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCrash) Color.Red else Color(0xFFFBC02D)
                    )
                }
                
                Text(
                    text = log.created_at?.substringBefore(".")?.replace("T", " ") ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.alpha(0.5f)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = log.place ?: "Fatal Crash",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = log.stack_trace,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.alpha(0.3f))
                    
                    RemoteInfoRow("App", log.app_version)
                    RemoteInfoRow("Device", log.device_info)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            context.copyToClipboard(log.stack_trace)
                            MessageUtils.showRemovableToast(context, R.string.copiedToClipboard, Toast.LENGTH_SHORT)
                        }) {
                            Icon(painterResource(R.drawable.icon_copy), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy Trace")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteInfoRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.alpha(0.7f)
        )
    }
}

@Composable
private fun AppLogItem(log: AppLogModel, isCrash: Boolean, handleDelete: (AppLogModel) -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = shapes.medium,
                spotColor = Color.Black.alpha(0.3f),
            )
            .background(MaterialTheme.colorScheme.surface, shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.alpha(0.5f), shapes.medium)
    ) {
        Column {
            Text(
                text = log.place,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.alpha(0.5f),
                thickness = 1.dp,
            )

            Text(
                text = log.logShort,
                color = if (isCrash) Color.Red else Color(0xFFB69200),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )

            HorizontalDivider(
                color = colorScheme.outlineVariant.alpha(0.5f),
                thickness = 1.dp,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = log.datetime,
                    modifier = Modifier.weight(1f),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurface,
                )
                SimpleTooltip(text = stringResource(R.string.strLabelDelete)) {
                    IconButton(
                        painter = painterResource(R.drawable.dr_icon_delete),
                        onClick = {
                            handleDelete(log)
                        },
                        tint = colorScheme.onSurface,
                        small = true,
                    )
                }
                SimpleTooltip(text = stringResource(R.string.strLabelCopy)) {
                    IconButton(
                        painter = painterResource(R.drawable.icon_copy),
                        onClick = {
                            context.copyToClipboard(log.log)
                            MessageUtils.showClipboardMessage(
                                context,
                                context.getString(R.string.copiedToClipboard),
                            )
                        },
                        tint = colorScheme.onSurface,
                        small = true,
                    )
                }
            }
        }
    }
}

