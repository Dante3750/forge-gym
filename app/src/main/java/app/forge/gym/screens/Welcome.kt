package app.forge.gym.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.forge.gym.LocalApp
import app.forge.gym.data.Gender
import app.forge.gym.ui.C
import app.forge.gym.ui.ForgeButton
import app.forge.gym.ui.IconBox
import app.forge.gym.ui.Segmented
import app.forge.gym.ui.T
import app.forge.gym.ui.Txt

/**
 * The only setup there is: your name, and whether the demos show a man or a woman.
 * Also used later (with [onBack]) to change either.
 */
@Composable
fun WelcomeScreen(onBack: (() -> Unit)? = null, onDone: () -> Unit) {
    val store = LocalApp.current.store
    val p = store.profile
    var name by rememberSaveable { mutableStateOf(p.name) }
    var gender by rememberSaveable { mutableIntStateOf(if (p.gender == Gender.FEMALE) 1 else 0) }

    Column(
        Modifier.fillMaxSize().background(C.bg).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(statusTop() + if (onBack == null) 72.dp else 12.dp))
        if (onBack != null) {
            IconBox(Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            Spacer(Modifier.height(20.dp))
        }
        Txt("FORGE", T.numXL, C.volt)
        Txt(if (onBack == null) "Log your workouts. See every exercise in 3D." else "Your details", T.h2)
        Spacer(Modifier.height(18.dp))
        Txt("WHAT'S YOUR NAME?", T.label, C.dim)
        NameField(name, { name = it })
        Spacer(Modifier.height(10.dp))
        Txt("SHOW EXERCISES WITH", T.label, C.dim)
        Segmented(listOf("A man", "A woman"), gender, { gender = it })
        Spacer(Modifier.height(18.dp))
        ForgeButton(
            if (onBack == null) "Start" else "Save",
            {
                store.updateProfile(p.copy(name = name.trim(), gender = if (gender == 1) Gender.FEMALE else Gender.MALE, onboarded = true))
                onDone()
            },
            Modifier.fillMaxWidth().navigationBarsPadding(),
            enabled = name.isNotBlank()
        )
        Spacer(Modifier.height(24.dp))
    }
}
