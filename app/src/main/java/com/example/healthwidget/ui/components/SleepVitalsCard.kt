package com.example.healthwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthwidget.model.SleepData
import com.example.healthwidget.model.VitalsData

@Composable
fun SleepVitalsCard(
    modifier: Modifier = Modifier,
    sleepData: SleepData = remember { SleepData.mock() },
    vitalsData: VitalsData = remember { VitalsData.mock() }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Upper Section: Sleep Card
            SleepWidgetCard(
                sleepData = sleepData
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Lower Section: Vitals & Nutrition Scrollable Row
            VitalsWidgetCard(
                vitalsData = vitalsData
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SleepVitalsCardPreview() {
    SleepVitalsCard(
        sleepData = SleepData.mock(),
        vitalsData = VitalsData.mock()
    )
}
