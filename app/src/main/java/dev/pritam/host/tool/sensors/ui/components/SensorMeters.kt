package dev.pritam.host.tool.sensors.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pritam.host.ui.theme.Amber
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.GlassBorder
import dev.pritam.host.ui.theme.GlassSurfaceDeep
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.TextTertiary
import dev.pritam.host.ui.theme.Violet
import java.util.Locale
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

// =========================================================================
// 1. Gyroscope Gimbal Rings (Pitch, Roll, Yaw rates)
// =========================================================================
@Composable
fun SensorGimbalRings(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    val wx = rawValues?.getOrNull(0) ?: 0f // rad/s Pitch rate
    val wy = rawValues?.getOrNull(1) ?: 0f // rad/s Roll rate
    val wz = rawValues?.getOrNull(2) ?: 0f // rad/s Yaw rate

    val totalAngularRate = remember(wx, wy, wz) {
        sqrt(wx * wx + wy * wy + wz * wz)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANGULAR VELOCITY GIMBAL",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(Locale.US, "RATE: %.2f rad/s", totalAngularRate),
                    color = Violet,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gimbal rings drawn in perspective
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer Yaw Ring (Violet) - wz
                    val rZ = size.width * 0.44f
                    val arcLenZ = (wz * 50f).coerceIn(-180f, 180f)
                    drawCircle(
                        color = GlassBorder,
                        radius = rZ,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = Violet,
                        startAngle = -90f,
                        sweepAngle = arcLenZ,
                        useCenter = false,
                        topLeft = Offset(center.x - rZ, center.y - rZ),
                        size = Size(rZ * 2, rZ * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Middle Pitch Ring (Cyan, perspective ellipse) - wx
                    val rX = size.width * 0.32f
                    drawOval(
                        color = GlassBorder.copy(alpha = 0.6f),
                        topLeft = Offset(center.x - rX, center.y - rX * 0.6f),
                        size = Size(rX * 2, rX * 1.2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    val arcLenX = (wx * 50f).coerceIn(-180f, 180f)
                    drawArc(
                        color = Cyan,
                        startAngle = 0f,
                        sweepAngle = arcLenX,
                        useCenter = false,
                        topLeft = Offset(center.x - rX, center.y - rX * 0.6f),
                        size = Size(rX * 2, rX * 1.2f),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Inner Roll Ring (Emerald, vertical perspective ellipse) - wy
                    val rY = size.width * 0.20f
                    drawOval(
                        color = GlassBorder.copy(alpha = 0.6f),
                        topLeft = Offset(center.x - rY * 0.6f, center.y - rY),
                        size = Size(rY * 1.2f, rY * 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    val arcLenY = (wy * 50f).coerceIn(-180f, 180f)
                    drawArc(
                        color = Emerald,
                        startAngle = -90f,
                        sweepAngle = arcLenY,
                        useCenter = false,
                        topLeft = Offset(center.x - rY * 0.6f, center.y - rY),
                        size = Size(rY * 1.2f, rY * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Core center point
                    drawCircle(color = TextPrimary, radius = 3.dp.toPx(), center = center)
                }

                // Center readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 95.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f °/s", Math.toDegrees(totalAngularRate.toDouble())),
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Axis breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GimbalAxisChip("X (Pitch)", wx, Cyan, Modifier.weight(1f))
                GimbalAxisChip("Y (Roll)", wy, Emerald, Modifier.weight(1f))
                GimbalAxisChip("Z (Yaw)", wz, Violet, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GimbalAxisChip(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(
                text = String.format(Locale.US, "%.2f", value),
                color = TextPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =========================================================================
// 2. Ambient Light Arch Gauge (Logarithmic 0 - 100,000 lux)
// =========================================================================
@Composable
fun SensorLuxGauge(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    val lux = rawValues?.firstOrNull() ?: 0f

    // Logarithmic fraction 0.0 .. 1.0 (from 1 lux to 100,000 lux)
    val fraction = remember(lux) {
        if (lux <= 0f) 0f
        else {
            val logVal = log10(lux.coerceAtLeast(1f)) // 0 (1 lux) .. 5 (100k lux)
            (logVal / 5f).coerceIn(0f, 1f)
        }
    }

    val luxTier = remember(lux) {
        when {
            lux < 10 -> "Dark / Night"
            lux < 100 -> "Dim Interior"
            lux < 500 -> "Office Light"
            lux < 2000 -> "Bright Room"
            lux < 20000 -> "Overcast Sky"
            else -> "Direct Sunlight"
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ILLUMINANCE GAUGE",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = luxTier,
                    color = Amber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 240-degree dial arc
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 12.dp.toPx()
                    val arcRect = Size(radius * 2, radius * 2)
                    val arcTopLeft = Offset(center.x - radius, center.y - radius)

                    val startAngle = 150f
                    val sweepRange = 240f

                    // Background track
                    drawArc(
                        color = GlassBorder,
                        startAngle = startAngle,
                        sweepAngle = sweepRange,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcRect,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active Illuminance Arc (Gradient Amber -> Yellow)
                    val activeSweep = sweepRange * fraction
                    if (activeSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to Amber,
                                0.5f to Color(0xFFFDE047),
                                1.0f to Amber
                            ),
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcRect,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Needle needle indicator
                    val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble()).toFloat()
                    val needleEnd = Offset(
                        center.x + (radius - 12.dp.toPx()) * cos(currentAngleRad),
                        center.y + (radius - 12.dp.toPx()) * sin(currentAngleRad)
                    )
                    drawLine(
                        color = TextPrimary,
                        start = center,
                        end = needleEnd,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Amber, radius = 4.dp.toPx(), center = center)
                }

                // Center value readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f", lux),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "LUX",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scale benchmark points
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 lx", color = TextTertiary, fontSize = 9.sp)
                Text("100 lx", color = TextTertiary, fontSize = 9.sp)
                Text("1k lx", color = TextTertiary, fontSize = 9.sp)
                Text("10k lx", color = TextTertiary, fontSize = 9.sp)
                Text("100k lx", color = TextTertiary, fontSize = 9.sp)
            }
        }
    }
}

// =========================================================================
// 3. Pressure & Altimeter Dial (hPa + calculated altitude)
// =========================================================================
@Composable
fun SensorAltimeterDial(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    val pressureHpa = rawValues?.firstOrNull() ?: 1013.25f

    // Standard barometric formula: altitude = 44330 * (1 - (p/1013.25)^0.190284)
    val estimatedAltitudeMeters = remember(pressureHpa) {
        val ratio = (pressureHpa / 1013.25).coerceAtLeast(0.01)
        (44330.0 * (1.0 - Math.pow(ratio, 0.190284))).toFloat()
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BAROMETRIC ALTIMETER",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(Locale.US, "QNH: %.1f hPa", pressureHpa),
                    color = Cyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dial
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 10.dp.toPx()

                    // Bezel
                    drawCircle(color = GlassBorder, radius = radius, center = center, style = Stroke(2.dp.toPx()))

                    // Altitude pointer angle: 0m = top (-90°), rotates 360° per 1000m
                    val altAngleDeg = -90f + (estimatedAltitudeMeters % 1000f) * 0.36f
                    val altRad = Math.toRadians(altAngleDeg.toDouble()).toFloat()

                    // Needle
                    val needleEnd = Offset(
                        center.x + (radius - 12.dp.toPx()) * cos(altRad),
                        center.y + (radius - 12.dp.toPx()) * sin(altRad)
                    )
                    drawLine(
                        color = Cyan,
                        start = center,
                        end = needleEnd,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = TextPrimary, radius = 4.dp.toPx(), center = center)
                }

                // Altitude readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f m", estimatedAltitudeMeters),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ALTITUDE MSL",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reference barometric info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Std Sea-Level: 1013.25 hPa", color = TextTertiary, fontSize = 9.sp)
                Text(
                    text = String.format(Locale.US, "%.1f ft", estimatedAltitudeMeters * 3.28084f),
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// =========================================================================
// 4. Proximity Sonar Radar (Distance cm / Near-Far)
// =========================================================================
@Composable
fun SensorSonarRadar(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    val distanceCm = rawValues?.firstOrNull() ?: 5f
    val isNear = distanceCm < 5f

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROXIMITY SONAR",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isNear) "STATUS: NEAR" else "STATUS: FAR",
                    color = if (isNear) Rose else Emerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sonar waves
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.width / 2f - 10.dp.toPx()

                    // Radar grid rings
                    for (i in 1..3) {
                        drawCircle(
                            color = GlassBorder.copy(alpha = 0.5f),
                            radius = maxR * (i / 3f),
                            center = center,
                            style = Stroke(1.dp.toPx())
                        )
                    }

                    // Pulsing animated wave
                    if (isNear) {
                        val pulseR = maxR * pulseWave
                        drawCircle(
                            color = Rose.copy(alpha = (1f - pulseWave) * 0.4f),
                            radius = pulseR,
                            center = center
                        )
                    }

                    // Distance object dot
                    val targetDistRatio = (distanceCm / 10f).coerceIn(0.1f, 1f)
                    val targetR = maxR * targetDistRatio
                    val targetColor = if (isNear) Rose else Emerald

                    drawCircle(
                        color = targetColor,
                        radius = 8.dp.toPx(),
                        center = Offset(center.x, center.y - targetR)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(center.x, center.y - targetR)
                    )

                    // Sensor receiver origin
                    drawCircle(color = TextPrimary, radius = 4.dp.toPx(), center = center)
                }

                // Numeric readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f cm", distanceCm),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =========================================================================
// 5. Thermal & Climate Gauge (Temperature °C / Humidity %)
// =========================================================================
@Composable
fun SensorThermalGauge(
    rawValues: List<Float>?,
    unit: String,
    modifier: Modifier = Modifier
) {
    val value = rawValues?.firstOrNull() ?: 24f
    val isHumidity = unit.contains("%")
    val minVal = if (isHumidity) 0f else -10f
    val maxVal = if (isHumidity) 100f else 60f
    val fraction = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHumidity) "HUMIDITY GAUGE" else "THERMAL GAUGE",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(Locale.US, "%.1f %s", value, unit),
                    color = if (isHumidity) Cyan else Amber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bar gauge with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(GlassBorder, RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(16.dp)
                        .background(
                            brush = if (isHumidity) {
                                Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.5f), Cyan))
                            } else {
                                Brush.horizontalGradient(listOf(Cyan, Amber, Rose))
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(String.format(Locale.US, "%.0f %s", minVal, unit), color = TextTertiary, fontSize = 9.sp)
                Text(String.format(Locale.US, "%.0f %s", maxVal, unit), color = TextTertiary, fontSize = 9.sp)
            }
        }
    }
}

// =========================================================================
// 6. Step Cadence Ring (Step Counter / Detector)
// =========================================================================
@Composable
fun SensorStepCadence(
    rawValues: List<Float>?,
    modifier: Modifier = Modifier
) {
    val steps = (rawValues?.firstOrNull() ?: 0f).toLong()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GlassSurfaceDeep,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PEDOMETER CADENCE",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "ACTIVE",
                    color = Emerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = size.width / 2f - 8.dp.toPx()

                    drawCircle(color = GlassBorder, radius = r, center = center, style = Stroke(6.dp.toPx()))

                    val progressSweep = ((steps % 10000) / 10000f) * 360f
                    drawArc(
                        color = Emerald,
                        startAngle = -90f,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$steps",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("STEPS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
