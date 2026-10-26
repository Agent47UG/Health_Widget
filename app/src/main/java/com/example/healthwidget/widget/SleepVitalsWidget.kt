package com.example.healthwidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.healthwidget.data.HealthRepository
import com.example.healthwidget.ui.components.SleepColors
import com.example.healthwidget.ui.components.VitalsColors
import com.example.healthwidget.ui.components.renderSleepGraphBitmap
import com.example.healthwidget.ui.components.renderVitalTrendBitmap

class SleepVitalsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HealthRepository(context)
        val sleepData = repository.getSleepData()
        val vitalsData = repository.getVitalsData()

        val sleepGraphBitmap = renderSleepGraphBitmap(
            sleepData = sleepData,
            widthPx = 800,
            heightPx = 300
        )

        provideContent {
            val prefs = currentState<Preferences>()
            val currentIndex = prefs[KEY_VITAL_INDEX] ?: 0
            val metricCount = vitalsData.metrics.size.coerceAtLeast(1)
            val metric = vitalsData.metrics[currentIndex % metricCount]

            val trendBitmap = renderVitalTrendBitmap(
                metric = metric,
                widthPx = 800,
                heightPx = 220
            )

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF141416), Color(0xFF141416)))
                    .padding(12.dp)
                    .clickable(actionRunCallback<OpenHealthAppAction>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // UPPER SECTION: Sleep
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    ColorProvider(
                                        Color(SleepColors.MAGENTA_TAG),
                                        Color(SleepColors.MAGENTA_TAG)
                                    )
                                )
                        ) {}

                        Spacer(modifier = GlanceModifier.width(6.dp))

                        Text(
                            text = "Sleep",
                            style = TextStyle(
                                color = ColorProvider(Color.White, Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sleepData.formattedDuration,
                            style = TextStyle(
                                color = ColorProvider(Color.White, Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Text(
                            text = sleepData.formattedTimeframe,
                            style = TextStyle(
                                color = ColorProvider(Color(SleepColors.TEXT_SECONDARY), Color(SleepColors.TEXT_SECONDARY)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = sleepData.sleepInsight,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFC7C7CC), Color(0xFFC7C7CC)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Image(
                        provider = ImageProvider(sleepGraphBitmap),
                        contentDescription = "Sleep Architecture Graph",
                        modifier = GlanceModifier.fillMaxWidth().height(145.dp)
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // DIVIDER
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProvider(Color(0xFF2C2C2E), Color(0xFF2C2C2E)))
                    ) {}

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // LOWER SECTION: Vitals Carousel
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    ColorProvider(
                                        Color(VitalsColors.ACCENT_GREEN),
                                        Color(VitalsColors.ACCENT_GREEN)
                                    )
                                )
                        ) {}

                        Spacer(modifier = GlanceModifier.width(6.dp))

                        Text(
                            text = metric.title,
                            style = TextStyle(
                                color = ColorProvider(Color.White, Color.White),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )

                        // Navigation Arrows: ❮ 1/9 ❯
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❮",
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.ACCENT_GREEN), Color(VitalsColors.ACCENT_GREEN)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clickable(actionRunCallback<PrevSleepVitalAction>())
                            )

                            Text(
                                text = "${(currentIndex % metricCount) + 1}/$metricCount",
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.TEXT_SECONDARY), Color(VitalsColors.TEXT_SECONDARY)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Text(
                                text = "❯",
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.ACCENT_GREEN), Color(VitalsColors.ACCENT_GREEN)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clickable(actionRunCallback<NextSleepVitalAction>())
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier
                                .padding(end = 8.dp)
                                .clickable(actionRunCallback<NextSleepVitalAction>())
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = metric.valueText,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White, Color.White),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(3.dp))
                                Text(
                                    text = metric.unitText,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White, Color.White),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            Text(
                                text = metric.statusText,
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.TEXT_SECONDARY), Color(VitalsColors.TEXT_SECONDARY)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }

                        Image(
                            provider = ImageProvider(trendBitmap),
                            contentDescription = metric.title,
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(58.dp)
                                .clickable(actionRunCallback<NextSleepVitalAction>())
                        )
                    }
                }
            }
        }
    }

    companion object {
        val KEY_VITAL_INDEX = intPreferencesKey("key_sleep_vital_index")
    }
}

class PrevSleepVitalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = HealthRepository(context)
        val vitalsData = repository.getVitalsData()
        val metricCount = vitalsData.metrics.size.coerceAtLeast(1)

        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[SleepVitalsWidget.KEY_VITAL_INDEX] ?: 0
            prefs[SleepVitalsWidget.KEY_VITAL_INDEX] = (current - 1 + metricCount) % metricCount
        }
        SleepVitalsWidget().update(context, glanceId)
    }
}

class NextSleepVitalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = HealthRepository(context)
        val vitalsData = repository.getVitalsData()
        val metricCount = vitalsData.metrics.size.coerceAtLeast(1)

        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[SleepVitalsWidget.KEY_VITAL_INDEX] ?: 0
            prefs[SleepVitalsWidget.KEY_VITAL_INDEX] = (current + 1) % metricCount
        }
        SleepVitalsWidget().update(context, glanceId)
    }
}

class CycleSleepVitalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NextSleepVitalAction().onAction(context, glanceId, parameters)
    }
}

class SleepVitalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SleepVitalsWidget()
}
