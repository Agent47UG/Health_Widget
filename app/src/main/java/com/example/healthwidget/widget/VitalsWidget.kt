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
import androidx.glance.action.actionStartActivity
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
import androidx.glance.layout.fillMaxHeight
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
import com.example.healthwidget.MainActivity
import com.example.healthwidget.data.HealthRepository
import com.example.healthwidget.ui.components.VitalsColors
import com.example.healthwidget.ui.components.renderVitalTrendBitmap

class VitalsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HealthRepository(context)
        val vitalsData = repository.getVitalsData()

        provideContent {
            val prefs = currentState<Preferences>()
            val currentIndex = prefs[KEY_METRIC_INDEX] ?: 0
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header Row: Green Tag + Title + Previous/Next Navigation Controls
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )

                        // Cycle Page Navigation: ❮ 1/9 ❯
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❮",
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.ACCENT_GREEN), Color(VitalsColors.ACCENT_GREEN)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clickable(actionRunCallback<PrevVitalAction>())
                            )

                            Text(
                                text = "${(currentIndex % metricCount) + 1}/$metricCount",
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.TEXT_SECONDARY), Color(VitalsColors.TEXT_SECONDARY)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Text(
                                text = "❯",
                                style = TextStyle(
                                    color = ColorProvider(Color(VitalsColors.ACCENT_GREEN), Color(VitalsColors.ACCENT_GREEN)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clickable(actionRunCallback<NextVitalAction>())
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    // Content Row: Value + Unit on Left, Graph expanding on Right
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier
                                .padding(end = 4.dp)
                                .clickable(actionRunCallback<NextVitalAction>())
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = metric.valueText,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White, Color.White),
                                        fontSize = 25.sp,
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

                            Spacer(modifier = GlanceModifier.height(1.dp))

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
                                .fillMaxHeight()
                                .clickable(actionRunCallback<NextVitalAction>())
                        )
                    }
                }
            }
        }
    }

    companion object {
        val KEY_METRIC_INDEX = intPreferencesKey("key_vital_metric_index")
    }
}

class PrevVitalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = HealthRepository(context)
        val vitalsData = repository.getVitalsData()
        val metricCount = vitalsData.metrics.size.coerceAtLeast(1)

        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[VitalsWidget.KEY_METRIC_INDEX] ?: 0
            prefs[VitalsWidget.KEY_METRIC_INDEX] = (current - 1 + metricCount) % metricCount
        }
        VitalsWidget().update(context, glanceId)
    }
}

class NextVitalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repository = HealthRepository(context)
        val vitalsData = repository.getVitalsData()
        val metricCount = vitalsData.metrics.size.coerceAtLeast(1)

        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[VitalsWidget.KEY_METRIC_INDEX] ?: 0
            prefs[VitalsWidget.KEY_METRIC_INDEX] = (current + 1) % metricCount
        }
        VitalsWidget().update(context, glanceId)
    }
}

class CycleVitalAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NextVitalAction().onAction(context, glanceId, parameters)
    }
}

class VitalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VitalsWidget()
}
