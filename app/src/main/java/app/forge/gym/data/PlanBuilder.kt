package app.forge.gym.data

import kotlin.math.abs

/**
 * Builds a weekly program from the exercise database.
 * Split depends on days per week, level and gender emphasis; exercise choice is
 * ranked (compound lifts first, classic movements favoured) and is deterministic,
 * so the same profile always gets the same plan until the user taps "Shuffle".
 */
object PlanBuilder {

    private const val Q = "quadriceps"
    private const val H = "hamstrings"
    private const val G = "glutes"
    private const val CALF = "calves"
    private const val CH = "chest"
    private const val LAT = "lats"
    private const val MID = "middle back"
    private const val SH = "shoulders"
    private const val BI = "biceps"
    private const val TRI = "triceps"
    private const val ABS = "abdominals"
    private const val LOW = "lower back"
    private const val TRAP = "traps"
    private const val ADD = "adductors"
    private const val ABD = "abductors"

    private class Slot(val muscles: List<String>, val count: Int = 1)

    private class DaySpec(val title: String, val slots: List<Slot>) {
        val focus: String
            get() = slots.map { prettyMuscle(it.muscles.first()) }.distinct().take(4).joinToString(" · ")
    }

    private fun s(vararg m: String, count: Int = 1) = Slot(m.toList(), count)


    private val goodWords = mapOf(
        "squat" to 4, "deadlift" to 4, "bench" to 4, "row" to 3, "pulldown" to 3, "pull-up" to 3,
        "pullup" to 3, "chin-up" to 3, "press" to 3, "curl" to 2, "lunge" to 3, "hip thrust" to 5,
        "thrust" to 3, "dip" to 2, "plank" to 3, "crunch" to 2, "raise" to 2, "push-up" to 3,
        "pushdown" to 2, "romanian" to 3, "leg press" to 3, "bridge" to 2, "glute" to 2, "extension" to 1,
        "fly" to 1, "step-up" to 2
    )

    private val badWords = listOf(
        "atlas", "tire", "axle", "sled", "yoke", "stone", "bosu", "chain", "hang", "snatch", "clean",
        "jerk", "grip", "wrist", "neck", "behind", "smith", "suspended", "stability", "one-arm",
        "single-arm", "reverse", "isometric", "partner", "log", "gorilla", "kneeling", "seated calf"
    )

    private fun levelsFor(level: Level): Set<String> = when (level) {
        Level.BEGINNER -> setOf("beginner")
        Level.INTERMEDIATE -> setOf("beginner", "intermediate")
        Level.HARD -> setOf("intermediate", "expert")
    }

    private fun score(e: Exercise, level: Level): Int {
        var s = 0
        if (e.isCompound) s += 3
        val n = e.name.lowercase()
        goodWords.forEach { (k, v) -> if (n.contains(k)) s += v }
        badWords.forEach { if (n.contains(it)) s -= 5 }
        when (e.equipment) {
            "barbell" -> s += if (level == Level.BEGINNER) -1 else 2
            "dumbbell" -> s += if (level == Level.BEGINNER) 2 else 2
            "machine" -> s += if (level == Level.BEGINNER) 3 else 1
            "cable" -> s += 1
        }
        val want = if (level == Level.BEGINNER) "beginner" else "intermediate"
        if (e.level == want) s += 2
        if (level == Level.HARD && e.level == "expert") s += 1
        return s
    }

    // ---- day templates ----------------------------------------------------

    private fun fullBody(days: Int, f: Boolean, hard: Boolean): List<DaySpec> {
        val n = if (hard) 2 else 1
        val a = DaySpec("Full body A", buildList {
            add(s(Q, count = n)); add(s(CH)); add(s(LAT)); add(s(SH)); add(s(ABS))
            if (f) add(s(G))
        })
        val b = DaySpec("Full body B", buildList {
            add(s(H)); add(s(if (f) G else Q)); add(s(MID, count = n)); add(s(TRI))
            if (!f) add(s(BI)) else add(s(SH))
            add(s(ABS))
        })
        val c = DaySpec("Full body C", buildList {
            add(s(Q, G)); add(s(CH, count = n)); add(s(LAT)); add(s(SH)); add(s(CALF)); add(s(ABS))
        })
        return listOf(a, b, c).take(days)
    }

    private fun upperLower(f: Boolean, hard: Boolean): List<DaySpec> {
        val n = if (hard) 2 else 1
        val upperA = DaySpec("Upper A", listOf(s(CH, count = n), s(LAT), s(MID), s(SH), s(BI), s(TRI)))
        val lowerA = DaySpec("Lower A", listOf(s(Q, count = n), s(H), s(G, count = if (f) 2 else 1), s(CALF), s(ABS)))
        val upperB = DaySpec("Upper B", listOf(s(SH, count = if (f) 1 else n), s(CH), s(LAT, count = n), s(MID), s(BI), s(TRI)))
        val lowerB = DaySpec(
            "Lower B",
            listOf(s(H, count = n), s(G, count = if (f) 2 else 1), s(Q), s(if (f) ABD else CALF), s(ABS))
        )
        return listOf(upperA, lowerA, upperB, lowerB)
    }

    private fun push(f: Boolean, hard: Boolean, title: String = "Push"): DaySpec =
        DaySpec(title, buildList {
            add(s(CH, count = if (f) 1 else 2)); add(s(SH, count = if (f) 2 else 1)); add(s(TRI))
            if (hard) add(s(CH))
        })

    private fun pull(hard: Boolean, title: String = "Pull"): DaySpec =
        DaySpec(title, buildList {
            add(s(LAT, count = 2)); add(s(MID)); add(s(BI))
            if (hard) add(s(TRAP))
        })

    private fun legs(f: Boolean, hard: Boolean, title: String = "Legs"): DaySpec =
        DaySpec(title, buildList {
            add(s(Q, count = 2)); add(s(H)); add(s(G, count = if (f) 2 else 1)); add(s(CALF))
            add(if (f) s(ABD, ADD) else s(ABS))
            if (hard) add(s(LOW))
        })

    private fun specs(p: Profile): List<DaySpec> {
        val f = p.gender == Gender.FEMALE
        val hard = p.level == Level.HARD
        val days = if (p.level == Level.BEGINNER) p.daysPerWeek.coerceIn(2, 4) else p.daysPerWeek.coerceIn(2, 6)
        return when {
            days <= 3 -> fullBody(days, f, hard)
            days == 4 -> upperLower(f, hard)
            days == 5 -> listOf(
                push(f, hard), pull(hard), legs(f, hard),
                DaySpec("Upper", listOf(s(CH), s(LAT), s(SH), s(MID), s(BI), s(TRI))),
                DaySpec("Lower + core", listOf(s(Q), s(H), s(G, count = if (f) 2 else 1), s(CALF), s(ABS, count = 2)))
            )
            else -> listOf(
                push(f, hard), pull(hard), legs(f, hard),
                push(f, hard, "Push B"), pull(hard, "Pull B"), legs(f, hard, "Legs B")
            )
        }
    }

    // ---- picking ----------------------------------------------------------

    private fun pick(pool: List<Exercise>, slot: Slot, p: Profile, used: Set<String>, dayIdx: Int, n: Int): Exercise? {
        val eq = p.place.equipment
        val lv = levelsFor(p.level)
        val byMuscle = pool.filter { e -> e.equipment in eq && e.primary.any { it in slot.muscles } }
        var cands = byMuscle.filter { it.level in lv }
        if (cands.isEmpty()) cands = byMuscle
        if (cands.isEmpty()) return null
        val ranked = cands.sortedWith(compareByDescending<Exercise> { score(it, p.level) }.thenBy { it.name })
        val fresh = ranked.filter { it.id !in used }
        val list = if (fresh.isNotEmpty()) fresh else ranked
        val window = list.take(3)
        val idx = if (p.planSeed == 0) 0 else abs((p.planSeed * 31 + dayIdx * 7 + n * 13) % window.size)
        return window[idx]
    }

    private fun prescribe(e: Exercise, p: Profile): PlanExercise {
        val comp = e.isCompound
        val base: Pair<Int, String> = when (p.goal) {
            Goal.STRENGTH -> if (comp) Pair(if (p.level == Level.HARD) 5 else 4, "4–6") else Pair(3, "8–10")
            Goal.BUILD -> Pair(if (comp && p.level == Level.HARD) 4 else 3, if (comp) "8–10" else "10–15")
            Goal.LOSE -> Pair(3, if (comp) "12–15" else "15–20")
            Goal.FIT -> Pair(3, "10–12")
        }
        val sets = if (p.level == Level.BEGINNER) minOf(base.first, 3) else base.first
        val rest = when (p.goal) {
            Goal.STRENGTH -> if (comp) 150 else 90
            Goal.LOSE -> 45
            else -> if (comp) 90 else 60
        }
        return PlanExercise(e.id, sets, base.second, rest)
    }

    private fun finisher(all: List<Exercise>, p: Profile, used: Set<String>): PlanExercise? {
        val cat = if (p.place.isHome) "plyometrics" else "cardio"
        val c = all.filter { it.category == cat && it.id !in used && (!p.place.isHome || it.equipment == "body only") }
            .sortedBy { it.name }
        if (c.isEmpty()) return null
        val e = c[abs(p.planSeed) % c.size]
        return PlanExercise(e.id, 1, "8–10 min", 0)
    }

    fun build(p: Profile, all: List<Exercise>): List<PlanDay> {
        if (all.isEmpty()) return emptyList()
        val pool = all.filter { it.category == "strength" || it.category == "powerlifting" || (p.place.isHome && it.category == "plyometrics") }
        val used = HashSet<String>()
        return specs(p).mapIndexed { dayIdx, spec ->
            val items = ArrayList<PlanExercise>()
            spec.slots.forEach { slot ->
                repeat(slot.count) { n ->
                    val e = pick(pool, slot, p, used, dayIdx, n)
                    if (e != null) {
                        used += e.id
                        items += prescribe(e, p)
                    }
                }
            }
            if (p.goal == Goal.LOSE) finisher(all, p, used)?.let { items += it }
            PlanDay(spec.title, spec.focus, items)
        }
    }

    fun firstNumber(text: String, fallback: Int = 10): Int =
        Regex("\\d+").find(text)?.value?.toIntOrNull() ?: fallback
}
