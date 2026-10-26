package com.example.healthwidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.healthwidget.MainActivity
import com.example.healthwidget.R
import com.example.healthwidget.data.HealthRepository
import com.example.healthwidget.ui.components.RingColors
import com.example.healthwidget.ui.components.renderHealthRingsBitmap
import com.example.healthwidget.ui.components.renderStepBarsBitmap

class HealthWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HealthRepository(context)
        val healthData = repository.getHealthData()

        val ringsBitmap = renderHealthRingsBitmap(
            context = context,
            healthData = healthData,
            sizePx = 512
        )

        val stepBarsBitmap = renderStepBarsBitmap(
            context = context,
            widthPx = 540,
            heightPx = 180,
            barRatios = healthData.stepBarRatios.toFloatArray()
        )

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF141416), Color(0xFF141416)))
                    .padding(14.dp)
                    .clickable(actionRunCallback<OpenHealthAppAction>())
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SECTION: Larger Standalone Concentric Activity Rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.padding(end = 12.dp)
                    ) {
                        Image(
                            provider = ImageProvider(ringsBitmap),
                            contentDescription = "Health Rings",
                            modifier = GlanceModifier.size(154.dp)
                        )
                    }

                    // RIGHT SECTION: Native Steps Module -> Calories & Exercise Subtext Rows
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Header: "Steps" with blue indicator bar tag, "Today" subtext, Shoe Icon on top right
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .background(ColorProvider(Color(RingColors.STEPS_PROGRESS), Color(RingColors.STEPS_PROGRESS)))
                                ) {}

                                Spacer(modifier = GlanceModifier.width(5.dp))

                                Column {
                                    Text(
                                        text = "Steps",
                                        style = TextStyle(
                                            color = ColorProvider(Color.White, Color.White),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Today",
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF8E8E93), Color(0xFF8E8E93)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Image(
                                provider = ImageProvider(R.drawable.ic_shoe),
                                contentDescription = "Steps",
                                modifier = GlanceModifier.size(16.dp),
                                colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF0088FF), Color(0xFF0088FF)))
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // 2. 7-Bar Chart Image (Bars only bitmap)
                        Image(
                            provider = ImageProvider(stepBarsBitmap),
                            contentDescription = "Steps Chart",
                            modifier = GlanceModifier.fillMaxWidth().height(46.dp)
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // 3. Step Count Total Row (Aligned horizontally with blue tag indicator)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = "${healthData.steps} ",
                                style = TextStyle(
                                    color = ColorProvider(Color.White, Color.White),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "steps",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8E8E93), Color(0xFF8E8E93)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // 4. Bottom Row: Calories, Exercise & Distance with Stacked Subtext
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Calories Column
                            GlanceStackedMetric(
                                barColor = Color(RingColors.CALORIES_PROGRESS),
                                label = "Calories",
                                valueSubtext = "${healthData.calories} / ${healthData.calorieGoal} kcal"
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            // Exercise Column
                            GlanceStackedMetric(
                                barColor = Color(RingColors.EXERCISE_PROGRESS),
                                label = "Exercise",
                                valueSubtext = "${healthData.exerciseMinutes} / ${healthData.exerciseGoal} min"
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            // Distance Badge
                            val formattedDistance = if (healthData.distanceKm % 1.0f == 0.0f) {
                                healthData.distanceKm.toInt().toString()
                            } else {
                                String.format("%.1f", healthData.distanceKm)
                            }

                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(Color(0xFF262628), Color(0xFF262628)))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$formattedDistance km",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFC7C7CC), Color(0xFFC7C7CC)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun GlanceStackedMetric(
    barColor: Color,
    label: String,
    valueSubtext: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(14.dp)
                .background(ColorProvider(barColor, barColor))
        ) {}

        Spacer(modifier = GlanceModifier.width(4.dp))

        Column {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(Color.White, Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = valueSubtext,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8E8E93), Color(0xFF8E8E93)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

class HealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthWidget()
}

class OpenHealthAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val pm = context.packageManager

        // 1. Candidate package names for Health & Fitness apps (Fitbit / Google Health primary)
        val healthPackages = listOf(
            "com.fitbit.FitbitMobile",                 // Fitbit app (Google Health)
            "com.google.android.apps.fitness",        // Google Fit
            "com.google.android.apps.healthdata",     // Health Connect APK
            "com.google.android.apps.health.services" // Google Health Services
        )

        for (pkg in healthPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: Exception) { }
            }
        }

        // 2. Fallback: Try launching Fitbit on Play Store or Web
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.fitbit.FitbitMobile")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
            return
        } catch (_: Exception) { }

        // 3. System / SDK Health Connect Settings Intent actions
        val healthConnectActions = listOf(
            "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS",
            "android.health.connect.action.HEALTH_CONNECT_SETTINGS",
            "android.settings.HEALTH_CONNECT_SETTINGS"
        )

        for (action in healthConnectActions) {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) { }
            }
        }

        // 3. Try launching Health Connect settings directly
        for (action in healthConnectActions) {
            try {
                val intent = Intent(action).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }

        // 4. Fallback: Open Google Health Connect on Play Store or Web
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.google.android.apps.healthdata")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // Final fallback: launch internal MainActivity
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mainIntent)
            }
        }
    }
}
