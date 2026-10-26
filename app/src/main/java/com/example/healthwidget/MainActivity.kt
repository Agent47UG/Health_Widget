package com.example.healthwidget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.health.connect.client.PermissionController
import com.example.healthwidget.data.HealthConnectManager
import com.example.healthwidget.data.HealthRepository
import com.example.healthwidget.model.HealthData
import com.example.healthwidget.model.SleepData
import com.example.healthwidget.model.VitalsData
import com.example.healthwidget.ui.components.HealthWidgetCard
import com.example.healthwidget.ui.components.SleepVitalsCard
import com.example.healthwidget.ui.components.SleepWidgetCard
import com.example.healthwidget.ui.components.VitalsWidgetCard
import com.example.healthwidget.ui.theme.HealthWIdgetTheme
import com.example.healthwidget.widget.HealthWidget
import com.example.healthwidget.widget.SleepVitalsWidget
import com.example.healthwidget.widget.SleepWidget
import com.example.healthwidget.widget.VitalsWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: HealthRepository
    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = HealthRepository(this)
        healthConnectManager = HealthConnectManager(this)

        setContent {
            HealthWIdgetTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF101012)
                ) {
                    HealthWidgetAppScreen(
                        repository = repository,
                        healthConnectManager = healthConnectManager,
                        onWidgetUpdate = {
                            lifecycleScopeUpdateWidget()
                        }
                    )
                }
            }
        }
    }

    private fun lifecycleScopeUpdateWidget() {
        val context = applicationContext
        kotlinx.coroutines.GlobalScope.launch {
            try {
                HealthWidget().updateAll(context)
                SleepWidget().updateAll(context)
                VitalsWidget().updateAll(context)
                SleepVitalsWidget().updateAll(context)
            } catch (_: Exception) { }
        }
    }
}

@Composable
fun HealthWidgetAppScreen(
    repository: HealthRepository,
    healthConnectManager: HealthConnectManager,
    onWidgetUpdate: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var healthData by remember { mutableStateOf(repository.getHealthData()) }
    var isPermissionsGranted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Goal input states
    var stepGoalInput by remember { mutableStateOf(healthData.stepGoal.toString()) }
    var calorieGoalInput by remember { mutableStateOf(healthData.calorieGoal.toString()) }
    var exerciseGoalInput by remember { mutableStateOf(healthData.exerciseGoal.toString()) }

    var sleepData by remember { mutableStateOf(SleepData.mock()) }
    var vitalsData by remember { mutableStateOf(VitalsData.mock()) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        coroutineScope.launch {
            isLoading = true
            isPermissionsGranted = healthConnectManager.hasAnyPermission()
            healthData = healthConnectManager.readTodayHealthData(healthData)
            sleepData = repository.getSleepData()
            vitalsData = repository.getVitalsData()
            repository.saveHealthData(healthData)
            onWidgetUpdate()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (healthConnectManager.isSdkAvailable()) {
            val hasAny = healthConnectManager.hasAnyPermission()
            isPermissionsGranted = hasAny
            if (hasAny) {
                coroutineScope.launch {
                    isLoading = true
                    healthData = healthConnectManager.readTodayHealthData(healthData)
                    sleepData = repository.getSleepData()
                    vitalsData = repository.getVitalsData()
                    repository.saveHealthData(healthData)
                    onWidgetUpdate()
                    isLoading = false
                }
            } else {
                sleepData = repository.getSleepData()
                vitalsData = repository.getVitalsData()
            }
        } else {
            sleepData = repository.getSleepData()
            vitalsData = repository.getVitalsData()
        }
    }

    Scaffold(
        containerColor = Color(0xFF101012)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Health Rings",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Home Screen Widget & Sync",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp
                    )
                }

                // Mode Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (healthData.isMockData) Color(0xFF3B220A) else Color(0xFF0A2A12)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (healthData.isMockData) "Demo Mode" else "Health Connect",
                        color = if (healthData.isMockData) Color(0xFFFF8C00) else Color(0xFF00E640),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Section 1: Live Widget Preview
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = Color(0xFF0088FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LIVE WIDGET PREVIEWS",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                HealthWidgetCard(healthData = healthData)

                SleepVitalsCard(sleepData = sleepData, vitalsData = vitalsData)
            }

            // Section 2: Health Connect & Data Source
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF0088FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Health Connect Integration",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF0088FF),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    // Toggle Demo Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Demo Data Mode",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Use exact values from reference design",
                                color = Color(0xFF8E8E93),
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = healthData.isMockData,
                            onCheckedChange = { isMock ->
                                val newData = healthData.copy(isMockData = isMock)
                                healthData = newData
                                repository.setMockMode(isMock)
                                repository.saveHealthData(newData)
                                coroutineScope.launch {
                                    sleepData = repository.getSleepData()
                                    vitalsData = repository.getVitalsData()
                                    onWidgetUpdate()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0088FF)
                            )
                        )
                    }

                    Divider(color = Color(0xFF2C2C2E))

                    if (!healthConnectManager.isSdkAvailable()) {
                        Text(
                            text = "Health Connect SDK is not available on this device.",
                            color = Color(0xFFFF453A),
                            fontSize = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isPermissionsGranted) Color(0xFF00E640) else Color(0xFFFF8C00),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPermissionsGranted) "Health Connect Connected" else "Permissions Needed",
                                        color = if (isPermissionsGranted) Color(0xFF00E640) else Color(0xFFFF8C00),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            requestPermissionLauncher.launch(healthConnectManager.requiredPermissions)
                                            healthConnectManager.openHealthConnectSettings()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPermissionsGranted) Color(0xFF2C2C2E) else Color(0xFF0088FF)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(if (isPermissionsGranted) "Permissions" else "Connect")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isLoading = true
                                                healthData = healthConnectManager.readTodayHealthData(healthData)
                                                sleepData = repository.getSleepData()
                                                vitalsData = repository.getVitalsData()
                                                repository.saveHealthData(healthData)
                                                onWidgetUpdate()
                                                isLoading = false
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Goals Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Customize Daily Goals",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoalTextField(
                            label = "Steps",
                            value = stepGoalInput,
                            onValueChange = { stepGoalInput = it },
                            modifier = Modifier.weight(1f),
                            iconColor = Color(0xFF0088FF)
                        )

                        GoalTextField(
                            label = "Calories (kcal)",
                            value = calorieGoalInput,
                            onValueChange = { calorieGoalInput = it },
                            modifier = Modifier.weight(1f),
                            iconColor = Color(0xFFFF8C00)
                        )

                        GoalTextField(
                            label = "Exercise (min)",
                            value = exerciseGoalInput,
                            onValueChange = { exerciseGoalInput = it },
                            modifier = Modifier.weight(1f),
                            iconColor = Color(0xFF00E640)
                        )
                    }

                    Button(
                        onClick = {
                            val newStepGoal = stepGoalInput.toIntOrNull() ?: healthData.stepGoal
                            val newCalorieGoal = calorieGoalInput.toIntOrNull() ?: healthData.calorieGoal
                            val newExerciseGoal = exerciseGoalInput.toIntOrNull() ?: healthData.exerciseGoal

                            val updated = healthData.copy(
                                stepGoal = newStepGoal,
                                calorieGoal = newCalorieGoal,
                                exerciseGoal = newExerciseGoal
                            )
                            healthData = updated
                            repository.saveHealthData(updated)
                            onWidgetUpdate()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262628)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Save Goals & Update Widget",
                            color = Color.White
                        )
                    }
                }
            }

            // Section 4: Home Screen Widget Setup Instructions
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "How to add Widget to Home Screen",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    InstructionStep(number = "1", text = "Touch and hold an empty space on your Home screen.")
                    InstructionStep(number = "2", text = "Tap Widgets and search for \"Health Rings Widget\".")
                    InstructionStep(number = "3", text = "Touch and drag the Health Rings Widget onto your Home screen.")
                }
            }
        }
    }
}

@Composable
private fun GoalTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(iconColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = iconColor,
                unfocusedBorderColor = Color(0xFF3A3A3C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF101012),
                unfocusedContainerColor = Color(0xFF101012)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF0088FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFFE5E5EA),
            fontSize = 13.sp
        )
    }
}
