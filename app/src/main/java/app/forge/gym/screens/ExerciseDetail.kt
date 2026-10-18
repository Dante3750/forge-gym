package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.data.formatWeight
import app.forge.gym.data.prettyMuscle
import app.forge.gym.data.unitLabel
import app.forge.gym.sharedKey
import app.forge.gym.ui.C
import app.forge.gym.ui.CutSm
import app.forge.gym.ui.CutXs
import app.forge.gym.ui.Motion
import app.forge.gym.ui.MotionPlayer
import app.forge.gym.ui.pressable
import app.forge.gym.ui.Empty
import app.forge.gym.ui.ExerciseImage
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Panel
import app.forge.gym.ui.SectionLabel
import app.forge.gym.ui.T
import app.forge.gym.ui.Tag
import app.forge.gym.ui.Txt
import kotlin.math.max

@Composable
private fun MiniToggle(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressable(onClick = onClick)
            .clip(CutXs)
            .background(if (on) C.volt else C.bg.copy(alpha = 0.72f))
            .border(1.dp, if (on) C.volt else C.line, CutXs)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Txt(text.uppercase(), T.label, if (on) C.bg else C.text) }
}

@Composable
fun ExerciseDetailScreen(id: String, onBack: () -> Unit, onOpen: (String) -> Unit = {}, onOpen3d: (String) -> Unit = {}) {
    val app = LocalApp.current
    val store = app.store
    val e = app.exercise(id)
    if (e == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (app.exercises.isEmpty()) Empty("Loading", "Getting the exercise library ready.")
                else Empty("Exercise not found", "It may have been renamed.")
                ForgeButton("Back", onBack, filled = false)
            }
        }
        return
    }
    val fav = id in store.favorites
    val best = store.bestOneRm()[id]
    val useKg = store.profile.useKg
    val pager = rememberPagerState { max(e.images.size, 1) }
    val motion = app.motionFor(e.id)
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var female by rememberSaveable { mutableStateOf(store.profile.gender == Gender.FEMALE) }
    var slow by rememberSaveable { mutableStateOf(false) }
    var angle by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(400.dp).sharedKey("ex-img-${e.id}")) {
                val photos: @Composable () -> Unit = {
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        ExerciseImage(e.imageUrl(page), Modifier.fillMaxSize(), scrim = true)
                    }
                }
                if (motion != null && mode == 0) {
                    app.forge.gym.viewer.Trainer(motion, female, Modifier.fillMaxSize(), angle = angle, speed = if (slow) 0.45f else 1f) {
                        ExerciseImage(e.imageUrl(0), Modifier.fillMaxSize(), scrim = true)
                    }
                } else photos()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = statusTop() + 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
                    IconBox(
                        if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        { store.toggleFavorite(e.id) }, tint = if (fav) C.volt else C.text
                    )
                }
                if (motion != null) {
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (mode == 0) {
                            MiniToggle(if (female) "Women" else "Men", true) { female = !female }
                            MiniToggle("0.5×", slow) { slow = !slow }
                            MiniToggle(if (angle == 0) "Angle 1" else "Angle 2", angle == 1) { angle = 1 - angle }
                        }
                        MiniToggle(if (mode == 0) "Photos" else "3D", false) { mode = 1 - mode }
                    }
                }
                if ((motion == null || mode == 1) && e.images.size > 1) {
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in e.images.indices) {
                            Box(
                                Modifier
                                    .size(width = if (pager.currentPage == i) 28.dp else 12.dp, height = 4.dp)
                                    .background(if (pager.currentPage == i) C.volt else C.dim.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
        if (motion != null) {
            item {
                ForgeButton(
                    "Open in 3D · turn & zoom", { onOpen3d(e.id) },
                    Modifier.padding(horizontal = 20.dp).padding(top = 14.dp).fillMaxWidth(),
                    icon = androidx.compose.material.icons.Icons.Rounded.ViewInAr
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)) {
                Txt(e.name.uppercase(), T.h1, maxLines = 3)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(e.levelLabel, if (e.level == "expert") C.ember else C.volt)
                    Tag(if (e.equipment == "none") "No gear" else e.equipment)
                    if (e.mechanic.isNotEmpty()) Tag(e.mechanic)
                }
            }
        }
        if (best != null) {
            item {
                Panel(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp).fillMaxWidth(), accent = true) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Txt("YOUR BEST (EST. 1RM)", T.label, C.bg)
                            Txt("Calculated from your logged sets", T.small, C.bg)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Txt(formatWeight(best, useKg), T.num, C.bg)
                            Spacer(Modifier.width(4.dp))
                            Txt(unitLabel(useKg).uppercase(), T.label, C.bg, Modifier.padding(bottom = 5.dp))
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 24.dp)) {
                SectionLabel("Muscles worked")
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(e.primary) { m ->
                        Box(
                            Modifier.clip(CutSm).background(C.volt).padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { Txt(prettyMuscle(m).uppercase(), T.label, C.bg) }
                    }
                    items(e.secondary) { m ->
                        Box(
                            Modifier.clip(CutSm).border(1.dp, C.line, CutSm).padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { Txt(prettyMuscle(m).uppercase(), T.label, C.dim) }
                    }
                }
            }
        }
        val ways = motion?.let { m -> Motion.ways(m) }.orEmpty()
        if (ways.size > 1) {
            item {
                Column(Modifier.padding(top = 26.dp)) {
                    SectionLabel("${ways.size} ways to train it", Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ways) { mv ->
                            val ex = if (mv == motion) e else app.exampleFor(mv, e.primary.firstOrNull(), e.id)
                            val here = mv == motion
                            Panel(Modifier.width(160.dp), accent = false, onClick = if (here || ex == null) null else ({ onOpen(ex.id) })) {
                                Column {
                                    MotionPlayer(mv, female, Modifier.fillMaxWidth().height(150.dp), hd = false, scrim = false) {
                                        ExerciseImage(ex?.imageUrl(0), Modifier.fillMaxSize())
                                    }
                                    Column(Modifier.padding(12.dp)) {
                                        Txt(Motion.wayLabel(mv).uppercase(), T.label, if (here) C.volt else C.dim)
                                        Spacer(Modifier.height(2.dp))
                                        Txt(if (here) "This one" else ex?.name ?: "", T.bodyM, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 28.dp)) {
                SectionLabel("How to do it")
                Spacer(Modifier.height(14.dp))
                e.steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(bottom = 16.dp)) {
                        Txt("%02d".format(i + 1), T.h2, C.volt, Modifier.width(40.dp))
                        Txt(step, T.body, C.text, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            ForgeButton(
                if (fav) "Saved" else "Save exercise", { store.toggleFavorite(e.id) },
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().navigationBarsPadding(),
                filled = !fav,
                icon = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
            )
        }
    }
}
