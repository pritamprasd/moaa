package dev.pritam.dynamictools.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.ui.theme.Cyan
import dev.pritam.dynamictools.ui.theme.GlassBorder
import dev.pritam.dynamictools.ui.theme.Rose
import dev.pritam.dynamictools.ui.theme.SurfaceDeep
import dev.pritam.dynamictools.ui.theme.SurfaceElevated
import dev.pritam.dynamictools.ui.theme.TextPrimary
import dev.pritam.dynamictools.ui.theme.TextSecondary
import dev.pritam.dynamictools.ui.theme.Violet

@Composable
fun DynamicToolCodeEditorDialog(
    bundle: DynamicToolBundle,
    onDismiss: () -> Unit,
    onSave: (html: String, css: String, js: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var htmlContent by remember { mutableStateOf(bundle.html) }
    var cssContent by remember { mutableStateOf(bundle.css) }
    var jsContent by remember { mutableStateOf(bundle.js) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDeep
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE CODE STUDIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Cyan,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Edit HTML, CSS, and JS with Instant Hot-Reload",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Rose, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Tabs: HTML / CSS / JS
                val tabs = listOf("HTML Structure", "CSS Styles", "JavaScript Logic")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = when (selectedTab) {
                                0 -> Rose
                                1 -> Cyan
                                else -> Violet
                            }
                        )
                    },
                    modifier = Modifier.border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) {
                                        when (index) {
                                            0 -> Rose
                                            1 -> Cyan
                                            else -> Violet
                                        }
                                    } else TextSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Code Editor TextArea
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val (content, onValueChange, accentColor) = when (selectedTab) {
                        0 -> Triple(htmlContent, { v: String -> htmlContent = v }, Rose)
                        1 -> Triple(cssContent, { v: String -> cssContent = v }, Cyan)
                        else -> Triple(jsContent, { v: String -> jsContent = v }, Violet)
                    }

                    OutlinedTextField(
                        value = content,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF030712), RoundedCornerShape(8.dp)),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color(0xFF030712),
                            unfocusedContainerColor = Color(0xFF030712)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            onSave(htmlContent, cssContent, jsContent)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("⚡ Apply & Live Reload", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
