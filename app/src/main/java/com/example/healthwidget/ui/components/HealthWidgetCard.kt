package com.example.healthwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthwidget.R
import com.example.healthwidget.model.HealthData

@Composable
fun HealthWidgetCard(
    healthData: HealthData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141416))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // LEFT SECTION: Larger Concentric Activity Rings Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                HealthRingsCanvas(
                    healthData = healthData,
                    modifier = Modifier.size(154.dp)
                )
            }

            // RIGHT SECTION: Steps Header -> Steps Bar Chart -> Calories & Exercise Subtext Rows
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Steps Module (No inner background)
                StepsBarChartModule(
                    steps = healthData.steps,
                    barRatios = healthData.stepBarRatios
                )

                // Calories, Exercise & Distance Row with Subtext metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Calories Column (Header + Subtext)
                    StackedMetricItem(
                        tagColor = Color(RingColors.CALORIES_PROGRESS),
                        label = "Calories",
                        valueSubtext = "${healthData.calories} / ${healthData.calorieGoal} kcal"
                    )

                    // Exercise Column (Header + Subtext)
                    StackedMetricItem(
                        tagColor = Color(RingColors.EXERCISE_PROGRESS),
                        label = "Exercise",
                        valueSubtext = "${healthData.exerciseMinutes} / ${healthData.exerciseGoal} min"
                    )

                    // Distance Badge Pill
                    val formattedDistance = if (healthData.distanceKm % 1.0f == 0.0f) {
                        healthData.distanceKm.toInt().toString()
                    } else {
                        String.format("%.1f", healthData.distanceKm)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF262628))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$formattedDistance km",
                            color = Color(0xFFC7C7CC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepsBarChartModule(
    steps: Int,
    barRatios: List<Float> = listOf(0.42f, 0.70f, 0.25f, 0.48f, 0.45f, 0.82f, 0.65f),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header ("Steps" with blue indicator tag, "Today" below, Shoe Icon on top right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Vertical Blue Tag Indicator
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(RingColors.STEPS_PROGRESS))
                )

                Spacer(modifier = Modifier.width(5.dp))

                Column {
                    Text(
                        text = "Steps",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Today",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_shoe),
                contentDescription = "Steps",
                tint = Color(0xFF0088FF),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 7-Bar Mini Chart (Seamless, taller for better visibility, aligned with "Today" text)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            barRatios.forEachIndexed { index, ratio ->
                val isCurrent = (index == barRatios.size - 1)
                Column(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF132238)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(ratio)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF0055B3)),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Top circular dot
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.5.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) Color.Transparent else Color(0xFF00C8FF))
                                    .then(
                                        if (isCurrent) Modifier.border(1.dp, Color(0xFF00C8FF), CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Step Count Total Text (Aligned horizontally with blue tag header)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("$steps ")
                    }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append("steps")
                    }
                }
            )
        }
    }
}

@Composable
private fun StackedMetricItem(
    tagColor: Color,
    label: String,
    valueSubtext: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Tag Indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tagColor)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Column {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueSubtext,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HealthWidgetCardPreview() {
    HealthWidgetCard(healthData = HealthData())
}
