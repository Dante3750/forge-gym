package app.forge.gym.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.forge.gym.LocalApp
import app.forge.gym.data.ActiveWalk
import app.forge.gym.data.WalkLog
import app.forge.gym.data.dayOf
import app.forge.gym.data.formatClock
import app.forge.gym.formatDay
import app.forge.gym.ui.C
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.Panel
import app.forge.gym.ui.Ring
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt
import app.forge.gym.ui.WeekBars
import app.forge.gym.ui.staggerIn
import kotlinx.coroutines.delay
import java.time.LocalDate

private fun grouped(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
fun WalkScreen() {
    val app = LocalApp.current
    val store = app.store
    val p = store.profile
    val ctx = LocalContext.current
    val view = LocalView.current

    val needsPermission = Build.VERSION.SDK_INT >= 29
    var granted by remember {
        mutableStateOf(
            !needsPermission ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    var counter by remember { mutableFloatStateOf(-1f) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val active = store.activeWalk

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    DisposableEffect(granted, sensor) {
        if (granted && sensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val v = event.values[0]
                    counter = v
                    val a = store.activeWalk
                    if (a != null && a.running && a.baseline < 0f) store.setActiveWalk(a.copy(baseline = v))
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(active?.running) {
        view.keepScreenOn = active?.running == true
        onDispose { view.keepScreenOn = false }
    }

    val liveSteps =
        if (active != null && active.running && active.baseline >= 0f && counter >= active.baseline) (counter - active.baseline).toInt() else 0
    val sessionSteps = if (active != null) active.accumSteps + liveSteps else 0
    val sessionMillis =
        if (active != null) active.accumMillis + (if (active.running) now - active.lastResumeAt else 0L) else 0L
    val stride = p.heightCm * 0.415f / 100f
    val weightKg = store.weights.lastOrNull()?.kg ?: 70f
    val distanceM = sessionSteps * stride
    val kcal = (sessionSteps * 0.045f * (weightKg / 70f)).toInt()
    val todayTotal = store.stepsToday() + (if (active != null && dayOf(active.startedAt) == LocalDate.now()) sessionSteps else 0)

    fun start() {
        val t = System.currentTimeMillis()
        store.setActiveWalk(ActiveWalk(t, true, 0, 0L, t, if (counter >= 0f) counter else -1f))
    }

    fun pause() {
        val a = active ?: return
        store.setActiveWalk(a.copy(running = false, accumSteps = sessionSteps, accumMillis = sessionMillis, lastResumeAt = 0L, baseline = -1f))
    }

    fun resume() {
        val a = active ?: return
        store.setActiveWalk(a.copy(running = true, lastResumeAt = System.currentTimeMillis(), baseline = if (counter >= 0f) counter else -1f))
    }

    fun finish() {
        val a = active ?: return
        if (sessionSteps > 0 || sessionMillis > 30_000L) {
            store.addWalk(WalkLog(a.startedAt, (sessionMillis / 1000).toInt(), sessionSteps, distanceM, kcal))
        }
        store.setActiveWalk(null)
    }

    val today = LocalDate.now()
    val weekValues = (6 downTo 0).map { back ->
        val d = today.minusDays(back.toLong())
        val saved = store.walks.filter { dayOf(it.start) == d }.sumOf { it.steps }
        val live = if (back == 0 && active != null && dayOf(active.startedAt) == d) sessionSteps else 0
        (saved + live).toFloat()
    }
    val weekLabels = (6 downTo 0).map { back -> today.minusDays(back.toLong()).dayOfWeek.name.take(1) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusTop() + 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(Modifier.staggerIn(0)) {
                Txt("WALK", T.hero)
                Txt("Counts steps with your phone's own sensor. No GPS, no battery drain.", T.small, C.dim)
            }
        }
        item {
            Box(Modifier.fillMaxWidth().staggerIn(1), contentAlignment = Alignment.Center) {
                Ring((todayTotal.toFloat() / p.stepGoal).coerceIn(0f, 1f), 280.dp, 16.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(grouped(todayTotal), T.numXL.copy(fontSize = 64.sp), maxLines = 1)
                        Txt("STEPS TODAY", T.label, C.dim)
                        Spacer(Modifier.height(6.dp))
                        Txt("GOAL ${grouped(p.stepGoal)}", T.label, C.volt)
                    }
                }
            }
        }
        if (sensor == null) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Txt("NO STEP SENSOR", T.label, C.ember)
                        Spacer(Modifier.height(6.dp))
                        Txt("This phone doesn't have a hardware step counter, so walks can't be counted here.", T.body, C.dim)
                    }
                }
            }
        } else if (!granted) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Txt("ONE PERMISSION", T.label, C.volt)
                        Spacer(Modifier.height(6.dp))
                        Txt("Android needs your OK before an app can read the step counter. Nothing leaves your phone.", T.body, C.dim)
                        Spacer(Modifier.height(14.dp))
                        ForgeButton(
                            "Allow step counting", { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            item {
                Column(Modifier.staggerIn(2)) {
                    if (active == null) {
                        ForgeButton("Start a walk", { start() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.DirectionsWalk)
                    } else {
                        Panel(Modifier.fillMaxWidth(), accent = active.running) {
                            Column(Modifier.padding(18.dp)) {
                                Txt(if (active.running) "WALKING" else "PAUSED", T.label, if (active.running) C.bg else C.volt)
                                Spacer(Modifier.height(6.dp))
                                Txt(formatClock((sessionMillis / 1000).toInt()), T.numXL.copy(fontSize = 64.sp), if (active.running) C.bg else C.text)
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    WalkFact(grouped(sessionSteps), "Steps", if (active.running) C.bg else C.text, active.running)
                                    WalkFact("%.2f".format(distanceM / 1000f), "Km", if (active.running) C.bg else C.text, active.running)
                                    WalkFact("$kcal", "Kcal", if (active.running) C.bg else C.text, active.running)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (active.running) {
                                ForgeButton("Pause", { pause() }, Modifier.weight(1f), filled = false, icon = Icons.Rounded.Pause)
                            } else {
                                ForgeButton("Resume", { resume() }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
                            }
                            ForgeButton("Finish", { finish() }, Modifier.weight(1f), filled = false, danger = true, icon = Icons.Rounded.Stop)
                        }
                        Spacer(Modifier.height(8.dp))
                        Txt("Keep Forge open while you walk. If you close it, the sensor keeps counting and Forge catches up when you come back.", T.small, C.faint)
                    }
                }
            }
        }
        item {
            Column(Modifier.staggerIn(3)) {
                SectionLabel("Last 7 days")
                Spacer(Modifier.height(14.dp))
                WeekBars(weekValues, weekLabels, goal = p.stepGoal.toFloat())
            }
        }
        if (store.walks.isNotEmpty()) {
            item { SectionLabel("Recent walks") }
            val recent = store.walks.take(6)
            items(recent.size) { i ->
                val w = recent[i]
                Panel(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Txt(formatDay(dayOf(w.start)).uppercase(), T.label, C.dim)
                            Txt("${grouped(w.steps)} steps", T.h2)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Txt("%.2f km".format(w.distanceM / 1000f), T.bodyM)
                            Txt(formatClock(w.durationSec) + " · ${w.kcal} kcal", T.small, C.dim)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkFact(value: String, label: String, color: androidx.compose.ui.graphics.Color, onAccent: Boolean) {
    Column {
        Txt(value, T.num, color)
        Txt(label.uppercase(), T.label, if (onAccent) C.bg else C.dim)
    }
}
