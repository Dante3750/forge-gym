package app.forge.gym.data

/**
 * Where weigh-ins come from. Manual entry works today.
 * A smart scale (Bluetooth, or a vendor cloud) plugs in later by implementing this
 * and registering it in [ScaleSources.all]; the Progress screen will list it automatically.
 */
interface ScaleSource {
    val id: String
    val label: String
    val available: Boolean
    val note: String
}

object ManualScale : ScaleSource {
    override val id = "manual"
    override val label = "Manual entry"
    override val available = true
    override val note = "Type in what the scale says."
}

object SmartScalePlaceholder : ScaleSource {
    override val id = "smart"
    override val label = "Smart scale"
    override val available = false
    override val note = "Coming soon. Connect your weighing machine and weigh-ins will land here on their own."
}

object ScaleSources {
    val all: List<ScaleSource> = listOf(ManualScale, SmartScalePlaceholder)
}
