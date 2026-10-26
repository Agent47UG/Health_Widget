package com.example.healthwidget.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.healthwidget.data.HealthRepository
import com.example.healthwidget.ui.components.SleepColors
import com.example.healthwidget.ui.components.renderSleepGraphBitmap

class SleepWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HealthRepository(context)
        val sleepData = repository.getSleepData()

        val sleepGraphBitmap = renderSleepGraphBitmap(
            sleepData = sleepData,
            widthPx = 800,
            heightPx = 400
        )

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF141416), Color(0xFF141416)))
                    .padding(14.dp)
                    .clickable(actionRunCallback<OpenHealthAppAction>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // 1. Header Row: Magenta indicator tag + "Sleep" Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(3.dp)
                                .height(18.dp)
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // 2. Duration & Timeframe Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sleepData.formattedDuration,
                            style = TextStyle(
                                color = ColorProvider(Color.White, Color.White),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Text(
                            text = sleepData.formattedTimeframe,
                            style = TextStyle(
                                color = ColorProvider(Color(SleepColors.TEXT_SECONDARY), Color(SleepColors.TEXT_SECONDARY)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Dynamic Natural Language Insight
                    Text(
                        text = sleepData.sleepInsight,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFC7C7CC), Color(0xFFC7C7CC)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // 3. Sleep Graph Bitmap (includes graph, time axis, and bottom stats legend)
                    Image(
                        provider = ImageProvider(sleepGraphBitmap),
                        contentDescription = "Sleep Architecture Graph",
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    )
                }
            }
        }
    }
}

class SleepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SleepWidget()
}
