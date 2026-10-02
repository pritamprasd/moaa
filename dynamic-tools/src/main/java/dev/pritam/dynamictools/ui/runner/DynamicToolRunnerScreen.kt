package dev.pritam.dynamictools.ui.runner

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.dynamictools.bridge.DynamicToolAndroidBridge
import dev.pritam.dynamictools.ui.components.DynamicToolCodeEditorDialog
import dev.pritam.dynamictools.ui.components.DynamicToolRefineDialog
import dev.pritam.dynamictools.ui.theme.Cyan
import dev.pritam.dynamictools.ui.theme.GlassBorder
import dev.pritam.dynamictools.ui.theme.Rose
import dev.pritam.dynamictools.ui.theme.SpaceBackground
import dev.pritam.dynamictools.ui.theme.SurfaceDeep
import dev.pritam.dynamictools.ui.theme.TextPrimary
import dev.pritam.dynamictools.ui.theme.TextSecondary
import dev.pritam.dynamictools.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DynamicToolRunnerScreen(
    toolId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DynamicToolRunnerViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val reloadTrigger by viewModel.reloadTrigger.collectAsStateWithLifecycle()
    val isRefining by viewModel.isRefining.collectAsStateWithLifecycle()
    val refinementStatus by viewModel.refinementStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showCodeEditor by remember { mutableStateOf(false) }
    var showRefineDialog by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableFloatStateOf(1f) }
    var wasRefining by remember { mutableStateOf(false) }

    LaunchedEffect(toolId) {
        viewModel.loadTool(toolId)
    }

    LaunchedEffect(isRefining, errorMessage) {
        if (wasRefining && !isRefining && errorMessage == null && showRefineDialog) {
            showRefineDialog = false
            Toast.makeText(context, "Tool successfully refined & updated!", Toast.LENGTH_SHORT).show()
        }
        wasRefining = isRefining
    }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (currentTool?.manifest?.displayName ?: "DYNAMIC TOOL").uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Cyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SANDBOXED WEB RUNNER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "v${currentTool?.manifest?.version ?: "1.0.0"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Refine with AI Button
                    IconButton(
                        onClick = { showRefineDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22A78BFA))
                                .border(BorderStroke(1.dp, Violet.copy(alpha = 0.5f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 14.sp)
                        }
                    }

                    // Live Code Editor Button
                    IconButton(
                        onClick = { showCodeEditor = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x2238BDF8))
                                .border(BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("</>", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Reload Button
                    IconButton(
                        onClick = { viewModel.reload() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x221E293B))
                                .border(BorderStroke(1.dp, GlassBorder), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔄", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pageProgress < 1f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Cyan,
                    trackColor = Color.Transparent
                )
            }

            if (currentTool == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tool not found or still loading...", color = TextSecondary)
                }
            } else {
                val bundle = currentTool!!

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                displayZoomControls = false
                                builtInZoomControls = false
                                setSupportZoom(false)
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }

                            setBackgroundColor(0xFF0B0F19.toInt())

                            // Add Android Bridge
                            val bridge = DynamicToolAndroidBridge(
                                context = ctx,
                                toolName = bundle.manifest.displayName,
                                onLogReceived = { msg ->
                                    // Forward to AppLogHub
                                }
                            )
                            addJavascriptInterface(bridge, "AndroidBridge")

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress / 100f
                                }

                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    return super.onConsoleMessage(consoleMessage)
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    pageProgress = 1f
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                }
                            }

                            webViewInstance = this
                            loadInlinedHtml(bundle)
                        }
                    },
                    update = { view ->
                        // Re-trigger load on update or reload
                        view.loadInlinedHtml(bundle)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpaceBackground)
                )
            }
        }
    }

    // Code Editor Modal
    if (showCodeEditor && currentTool != null) {
        DynamicToolCodeEditorDialog(
            bundle = currentTool!!,
            onDismiss = { showCodeEditor = false },
            onSave = { html, css, js ->
                viewModel.updateCode(html, css, js)
            }
        )
    }

    // AI Refine Modal
    if (showRefineDialog && currentTool != null) {
        DynamicToolRefineDialog(
            bundle = currentTool!!,
            isRefining = isRefining,
            refinementStatus = refinementStatus,
            errorMessage = errorMessage,
            onDismiss = {
                showRefineDialog = false
                viewModel.clearError()
            },
            onRefine = { prompt ->
                viewModel.refineWithAi(prompt)
            }
        )
    }
}

private fun WebView.loadInlinedHtml(bundle: dev.pritam.dynamictools.model.DynamicToolBundle) {
    val htmlData = bundle.buildInlinedHtml()
    val baseUrl = if (bundle.rootDirectoryPath.isNotBlank()) "file://${bundle.rootDirectoryPath}/" else "file:///android_asset/"
    loadDataWithBaseURL(baseUrl, htmlData, "text/html", "UTF-8", null)
}
