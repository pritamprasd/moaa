package dev.pritam.host.tool.nettopology.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.tool.nettopology.model.*
import dev.pritam.host.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun TopologyCanvasGraph(
    graph: TopologyGraph,
    layoutMode: GraphLayoutMode,
    selectedNodeId: String?,
    onSelectNode: (NetworkNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.4f, 3.5f)
        offset += offsetChange
    }

    // Infinite transition for edge signal pulse animations & gateway halo
    val infiniteTransition = rememberInfiniteTransition(label = "TopologyPulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    val haloRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloRadius"
    )

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Node Positions Map computed based on layout mode
    val nodePositions = remember(graph, layoutMode) {
        computeNodePositions(graph, layoutMode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF070B14))
            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(14.dp))
            .transformable(state = transformState)
            .pointerInput(graph, nodePositions, scale, offset) {
                detectTapGestures { tapOffset ->
                    // Transform tap coordinates back to graph canvas space
                    val canvasCenterX = size.width / 2f
                    val canvasCenterY = size.height / 2f

                    val unscaledX = (tapOffset.x - canvasCenterX - offset.x) / scale
                    val unscaledY = (tapOffset.y - canvasCenterY - offset.y) / scale

                    // Find closest node within touch radius (~40dp)
                    val touchRadius = 45f
                    var clickedNode: NetworkNode? = null
                    var minDistance = Float.MAX_VALUE

                    for ((node, pos) in nodePositions) {
                        val dist = hypot(unscaledX - pos.x, unscaledY - pos.y)
                        if (dist <= touchRadius && dist < minDistance) {
                            minDistance = dist
                            clickedNode = node
                        }
                    }

                    clickedNode?.let { onSelectNode(it) }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasCenterX = size.width / 2f
            val canvasCenterY = size.height / 2f

            // Draw Background Cyber Grid Matrix
            drawCyberGrid(canvasCenterX, canvasCenterY, offset, scale)

            // Draw Edges & Animated Signal Particles
            for (edge in graph.edges) {
                val fromNode = graph.nodes.firstOrNull { it.ip == edge.fromIp }
                val toNode = graph.nodes.firstOrNull { it.ip == edge.toIp }

                val fromPos = nodePositions[fromNode]
                val toPos = nodePositions[toNode]

                if (fromPos != null && toPos != null) {
                    val p1 = Offset(canvasCenterX + offset.x + fromPos.x * scale, canvasCenterY + offset.y + fromPos.y * scale)
                    val p2 = Offset(canvasCenterX + offset.x + toPos.x * scale, canvasCenterY + offset.y + toPos.y * scale)

                    // Draw connecting line with subtle gradient glow
                    val edgeColor = if (toNode?.id == selectedNodeId || fromNode?.id == selectedNodeId) Cyan else Color(0x350284C7)
                    val edgeStroke = if (toNode?.id == selectedNodeId || fromNode?.id == selectedNodeId) 2.5f * scale else 1.5f * scale

                    drawLine(
                        color = edgeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = edgeStroke,
                        cap = StrokeCap.Round
                    )

                    // Draw Animated Flowing Signal Particle
                    val particleT = (pulsePhase + (edge.fromIp.hashCode() % 10) * 0.1f) % 1f
                    val particleX = p1.x + (p2.x - p1.x) * particleT
                    val particleY = p1.y + (p2.y - p1.y) * particleT

                    drawCircle(
                        color = Cyan.copy(alpha = 0.85f),
                        radius = 3.5f * scale,
                        center = Offset(particleX, particleY)
                    )
                }
            }

            // Draw Nodes
            for ((node, rawPos) in nodePositions) {
                val nodePos = Offset(
                    canvasCenterX + offset.x + rawPos.x * scale,
                    canvasCenterY + offset.y + rawPos.y * scale
                )

                val isSelected = node.id == selectedNodeId
                val nodeColor = node.deviceType.defaultColor

                // 1. Gateway or Selected Halo
                if (node.isGateway || isSelected) {
                    drawCircle(
                        color = (if (isSelected) Rose else nodeColor).copy(alpha = 0.18f),
                        radius = (haloRadius + if (isSelected) 8f else 0f) * scale,
                        center = nodePos
                    )
                }

                // 2. Node Core Disc
                val baseRadius = when {
                    node.isGateway -> 22f
                    node.isLocalDevice -> 18f
                    node.deviceType == NetworkDeviceType.SUB_ROUTER -> 18f
                    else -> 15f
                } * scale

                drawCircle(
                    color = GlassSurfaceElevated,
                    radius = baseRadius,
                    center = nodePos
                )

                // 3. Node Outer Rim / Border
                drawCircle(
                    color = if (isSelected) Rose else nodeColor,
                    radius = baseRadius,
                    center = nodePos,
                    style = Stroke(width = if (isSelected) 3f * scale else 2f * scale)
                )

                // 4. Latency / Status Indicator Dot
                val statusColor = when {
                    !node.isOnline -> Rose
                    (node.latencyMs ?: 999) < 15 -> Color(0xFF34D399) // Emerald Green
                    (node.latencyMs ?: 999) < 60 -> Color(0xFFFBBF24) // Yellow
                    else -> Color(0xFFF87171)
                }
                drawCircle(
                    color = statusColor,
                    radius = 4f * scale,
                    center = Offset(nodePos.x + baseRadius * 0.7f, nodePos.y - baseRadius * 0.7f)
                )

                // 5. Node Text Label (Emoji & IP Address)
                if (scale >= 0.55f) {
                    val emojiText = node.deviceType.emoji
                    val emojiLayout = textMeasurer.measure(
                        text = AnnotatedString(emojiText),
                        style = TextStyle(fontSize = (if (node.isGateway) 14 else 11).sp * scale)
                    )
                    drawText(
                        textLayoutResult = emojiLayout,
                        topLeft = Offset(
                            nodePos.x - emojiLayout.size.width / 2f,
                            nodePos.y - emojiLayout.size.height / 2f
                        )
                    )

                    // Node Title / IP beneath node
                    val labelText = if (node.isGateway) "GATEWAY\n${node.ip}" else if (node.isLocalDevice) "THIS DEVICE\n${node.ip}" else node.ip
                    val labelLayout = textMeasurer.measure(
                        text = AnnotatedString(labelText),
                        style = TextStyle(
                            fontSize = 8.5.sp * scale,
                            color = if (isSelected) Rose else TextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(
                            nodePos.x - labelLayout.size.width / 2f,
                            nodePos.y + baseRadius + 4f * scale
                        )
                    )
                }
            }
        }

        // Overlay Controls: Zoom In / Zoom Out / Reset Center
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = { scale = (scale * 1.25f).coerceAtMost(3.5f) },
                shape = CircleShape,
                color = GlassSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = Cyan, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            Surface(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.4f) },
                shape = CircleShape,
                color = GlassSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("-", color = Cyan, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                shape = RoundedCornerShape(8.dp),
                color = GlassSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.height(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text("CENTER", color = TextSecondary, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }

        // Top Legend Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color(0x990A0F1D), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0x2238BDF8)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${graph.nodes.size} Nodes", color = Cyan, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("•", color = TextSecondary, fontSize = 9.sp)
            Text(layoutMode.displayName, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

/**
 * Computes 2D coordinates (X, Y) for each node relative to the graph center.
 */
private fun computeNodePositions(
    graph: TopologyGraph,
    layoutMode: GraphLayoutMode
): Map<NetworkNode, Offset> {
    val positions = mutableMapOf<NetworkNode, Offset>()
    val nodes = graph.nodes
    if (nodes.isEmpty()) return positions

    val root = nodes.firstOrNull { it.isGateway } ?: nodes.first()

    when (layoutMode) {
        GraphLayoutMode.HIERARCHICAL -> {
            // Root Gateway at Top Center
            positions[root] = Offset(0f, -180f)

            val otherNodes = nodes.filter { it != root }
            val subRouters = otherNodes.filter { it.deviceType == NetworkDeviceType.SUB_ROUTER }
            val directLeaves = otherNodes.filter { it.deviceType != NetworkDeviceType.SUB_ROUTER }

            // Middle tier: Sub-routers or Local Device
            if (subRouters.isNotEmpty()) {
                val subCount = subRouters.size
                val subSpacing = 220f
                val startX = -((subCount - 1) * subSpacing) / 2f
                subRouters.forEachIndexed { i, sub ->
                    positions[sub] = Offset(startX + i * subSpacing, -40f)
                }
            }

            // Bottom tier: Leaf devices arranged symmetrically in columns/rows
            val leafCount = directLeaves.size
            if (leafCount > 0) {
                val cols = ceil(sqrt(leafCount.toDouble())).toInt().coerceIn(3, 8)
                val spacingX = 130f
                val spacingY = 110f

                directLeaves.forEachIndexed { index, leaf ->
                    val row = index / cols
                    val col = index % cols
                    val itemsInRow = min(cols, leafCount - row * cols)
                    val rowStartX = -((itemsInRow - 1) * spacingX) / 2f
                    val posX = rowStartX + col * spacingX
                    val posY = (if (subRouters.isNotEmpty()) 100f else -20f) + row * spacingY
                    positions[leaf] = Offset(posX, posY)
                }
            }
        }

        GraphLayoutMode.RADIAL -> {
            // Root Gateway at Center (0,0)
            positions[root] = Offset.Zero

            val otherNodes = nodes.filter { it != root }
            val subRouters = otherNodes.filter { it.deviceType == NetworkDeviceType.SUB_ROUTER || it.isLocalDevice }
            val outerLeaves = otherNodes.filter { !subRouters.contains(it) }

            // Inner Orbit (Radius 140)
            val innerRadius = 140f
            if (subRouters.isNotEmpty()) {
                val step = 2 * PI / subRouters.size
                subRouters.forEachIndexed { i, node ->
                    val angle = i * step - PI / 2
                    positions[node] = Offset((innerRadius * cos(angle)).toFloat(), (innerRadius * sin(angle)).toFloat())
                }
            }

            // Outer Orbit (Radius 280..340)
            val outerRadius = if (subRouters.isNotEmpty()) 280f else 220f
            if (outerLeaves.isNotEmpty()) {
                val step = 2 * PI / outerLeaves.size
                outerLeaves.forEachIndexed { i, node ->
                    val angle = i * step - PI / 2
                    val radiusVariation = outerRadius + (i % 2) * 45f
                    positions[node] = Offset((radiusVariation * cos(angle)).toFloat(), (radiusVariation * sin(angle)).toFloat())
                }
            }
        }

        GraphLayoutMode.MESH -> {
            // Concentric concentric rings
            positions[root] = Offset(0f, -80f)
            val otherNodes = nodes.filter { it != root }

            val total = otherNodes.size
            val numRings = if (total > 16) 3 else if (total > 8) 2 else 1
            var nodeIdx = 0

            for (ring in 1..numRings) {
                val ringRadius = ring * 135f
                val countInRing = min(total - nodeIdx, ring * 7)
                if (countInRing <= 0) break

                val step = 2 * PI / countInRing
                for (i in 0 until countInRing) {
                    val node = otherNodes[nodeIdx++]
                    val angle = i * step
                    positions[node] = Offset((ringRadius * cos(angle)).toFloat(), (ringRadius * sin(angle)).toFloat())
                }
            }
        }
    }

    return positions
}

private fun DrawScope.drawCyberGrid(centerX: Float, centerY: Float, offset: Offset, scale: Float) {
    val gridSize = 40f * scale
    val width = size.width
    val height = size.height

    val startX = ((centerX + offset.x) % gridSize)
    val startY = ((centerY + offset.y) % gridSize)

    var x = startX
    while (x < width) {
        drawLine(
            color = Color(0x0C38BDF8),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += gridSize
    }

    var y = startY
    while (y < height) {
        drawLine(
            color = Color(0x0C38BDF8),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}
