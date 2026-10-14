package app.forge.gym.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

/** Everything the user creates lives here, saved as small JSON blobs on the phone. No account, no server. */
class Store(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("forge_store", Context.MODE_PRIVATE)

    var profile by mutableStateOf(readProfile())
        private set

    val workouts = mutableStateListOf<WorkoutLog>().apply { addAll(readWorkouts()) }
    val weights = mutableStateListOf<WeightEntry>().apply { addAll(readWeights()) }
    val walks = mutableStateListOf<WalkLog>().apply { addAll(readWalks()) }
    val favorites = mutableStateListOf<String>().apply { addAll(readFavs()) }
    private val water = mutableStateMapOf<String, Int>().apply { putAll(readWater()) }

    var activeWalk by mutableStateOf(readActiveWalk())
        private set

    // ---- mutations --------------------------------------------------------

    fun updateProfile(p: Profile) {
        profile = p
        sp.edit().putString("profile", profileToJson(p).toString()).apply()
    }

    fun addWorkout(w: WorkoutLog) {
        workouts.add(0, w)
        sp.edit().putString("workouts", JSONArray().also { a -> workouts.forEach { a.put(workoutToJson(it)) } }.toString()).apply()
    }

    fun deleteWorkout(id: Long) {
        workouts.removeAll { it.id == id }
        sp.edit().putString("workouts", JSONArray().also { a -> workouts.forEach { a.put(workoutToJson(it)) } }.toString()).apply()
    }

    fun addWeight(kg: Float, source: String = "manual", time: Long = System.currentTimeMillis()) {
        weights.add(WeightEntry(time, kg, source))
        weights.sortBy { it.time }
        saveWeights()
    }

    fun deleteWeight(time: Long) {
        weights.removeAll { it.time == time }
        saveWeights()
    }

    private fun saveWeights() {
        sp.edit().putString("weights", JSONArray().also { a ->
            weights.forEach { a.put(JSONObject().put("t", it.time).put("kg", it.kg.toDouble()).put("src", it.source)) }
        }.toString()).apply()
    }

    fun addWalk(w: WalkLog) {
        walks.add(0, w)
        sp.edit().putString("walks", JSONArray().also { a ->
            walks.forEach {
                a.put(
                    JSONObject().put("s", it.start).put("d", it.durationSec).put("steps", it.steps)
                        .put("m", it.distanceM.toDouble()).put("kcal", it.kcal)
                )
            }
        }.toString()).apply()
    }

    fun toggleFavorite(id: String) {
        if (id in favorites) favorites.remove(id) else favorites.add(id)
        sp.edit().putString("favs", JSONArray(favorites.toList()).toString()).apply()
    }

    fun waterToday(): Int = water[LocalDate.now().toString()] ?: 0

    fun addWater(ml: Int) {
        val key = LocalDate.now().toString()
        water[key] = ((water[key] ?: 0) + ml).coerceAtLeast(0)
        val o = JSONObject()
        water.forEach { (k, v) -> o.put(k, v) }
        sp.edit().putString("water", o.toString()).apply()
    }

    fun saveActiveWalk(a: ActiveWalk?) {
        activeWalk = a
        if (a == null) {
            sp.edit().remove("activeWalk").apply()
        } else {
            sp.edit().putString(
                "activeWalk",
                JSONObject().put("startedAt", a.startedAt).put("running", a.running).put("accumSteps", a.accumSteps)
                    .put("accumMillis", a.accumMillis).put("lastResumeAt", a.lastResumeAt)
                    .put("baseline", a.baseline.toDouble()).toString()
            ).apply()
        }
    }

    fun resetAll() {
        sp.edit().clear().apply()
        profile = Profile()
        workouts.clear(); weights.clear(); walks.clear(); favorites.clear(); water.clear()
        activeWalk = null
    }

    // ---- derived stats ----------------------------------------------------

    fun stepsToday(): Int {
        val today = LocalDate.now()
        return walks.filter { dayOf(it.start) == today }.sumOf { it.steps }
    }

    fun activityDays(): Set<LocalDate> {
        val days = HashSet<LocalDate>()
        workouts.forEach { days += dayOf(it.startedAt) }
        walks.filter { it.durationSec >= 600 }.forEach { days += dayOf(it.start) }
        return days
    }

    fun streak(): Int {
        val days = activityDays()
        var d = LocalDate.now()
        if (d !in days) d = d.minusDays(1)
        var n = 0
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun workoutsThisWeek(): Int {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        return workouts.count { !dayOf(it.startedAt).isBefore(monday) }
    }

    fun trainedToday(): Boolean = workouts.any { dayOf(it.startedAt) == LocalDate.now() }

    /** Best estimated one-rep max per exercise (Epley). */
    fun bestOneRm(): Map<String, Float> {
        val best = HashMap<String, Float>()
        workouts.forEach { w ->
            w.exercises.forEach { ex ->
                ex.sets.forEach { s ->
                    if (s.weightKg > 0f) {
                        val est = s.weightKg * (1f + s.reps / 30f)
                        if (est > (best[ex.exerciseId] ?: 0f)) best[ex.exerciseId] = est
                    }
                }
            }
        }
        return best
    }

    fun lastLogFor(exerciseId: String): SetLog? {
        for (w in workouts) {
            val ex = w.exercises.firstOrNull { it.exerciseId == exerciseId } ?: continue
            if (ex.sets.isNotEmpty()) return ex.sets.last()
        }
        return null
    }

    /** 0..1 intensity per muscle from the last 7 days of training. */
    fun muscleHeat(lookup: (String) -> Exercise?): Map<String, Float> {
        val since = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val raw = HashMap<String, Float>()
        workouts.filter { it.startedAt >= since }.forEach { w ->
            w.exercises.forEach { log ->
                val e = lookup(log.exerciseId) ?: return@forEach
                e.primary.forEach { raw[it] = (raw[it] ?: 0f) + log.sets.size }
                e.secondary.forEach { raw[it] = (raw[it] ?: 0f) + log.sets.size * 0.4f }
            }
        }
        val max = raw.values.maxOrNull() ?: return emptyMap()
        if (max <= 0f) return emptyMap()
        return raw.mapValues { (it.value / max).coerceIn(0f, 1f) }
    }

    // ---- reading ----------------------------------------------------------

    private fun readProfile(): Profile {
        val s = sp.getString("profile", null) ?: return Profile()
        return try {
            val o = JSONObject(s)
            Profile(
                name = o.optString("name", ""),
                gender = runCatching { Gender.valueOf(o.optString("gender", "MALE")) }.getOrDefault(Gender.MALE),
                level = runCatching { Level.valueOf(o.optString("level", "BEGINNER")) }.getOrDefault(Level.BEGINNER),
                goal = runCatching { Goal.valueOf(o.optString("goal", "BUILD")) }.getOrDefault(Goal.BUILD),
                place = runCatching { Place.valueOf(o.optString("place", "GYM")) }.getOrDefault(Place.GYM),
                daysPerWeek = o.optInt("days", 3),
                heightCm = o.optInt("height", 172),
                goalWeightKg = o.optDouble("goalKg", 0.0).toFloat(),
                useKg = o.optBoolean("useKg", true),
                stepGoal = o.optInt("stepGoal", 8000),
                waterGoalMl = o.optInt("waterGoal", 2500),
                planSeed = o.optInt("seed", 0),
                onboarded = o.optBoolean("onboarded", false)
            )
        } catch (e: Exception) {
            Profile()
        }
    }

    private fun profileToJson(p: Profile) = JSONObject()
        .put("name", p.name).put("gender", p.gender.name).put("level", p.level.name)
        .put("goal", p.goal.name).put("place", p.place.name).put("days", p.daysPerWeek)
        .put("height", p.heightCm).put("goalKg", p.goalWeightKg.toDouble()).put("useKg", p.useKg)
        .put("stepGoal", p.stepGoal).put("waterGoal", p.waterGoalMl).put("seed", p.planSeed)
        .put("onboarded", p.onboarded)

    private fun workoutToJson(w: WorkoutLog): JSONObject {
        val ex = JSONArray()
        w.exercises.forEach { e ->
            val sets = JSONArray()
            e.sets.forEach { sets.put(JSONObject().put("r", it.reps).put("w", it.weightKg.toDouble())) }
            ex.put(JSONObject().put("id", e.exerciseId).put("name", e.name).put("sets", sets))
        }
        return JSONObject().put("id", w.id).put("title", w.title).put("at", w.startedAt)
            .put("dur", w.durationSec).put("ex", ex)
    }

    private fun readWorkouts(): List<WorkoutLog> {
        val s = sp.getString("workouts", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val exArr = o.getJSONArray("ex")
                WorkoutLog(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    startedAt = o.getLong("at"),
                    durationSec = o.getInt("dur"),
                    exercises = List(exArr.length()) { j ->
                        val e = exArr.getJSONObject(j)
                        val sa = e.getJSONArray("sets")
                        ExerciseLog(
                            e.getString("id"), e.getString("name"),
                            List(sa.length()) { k ->
                                val so = sa.getJSONObject(k)
                                SetLog(so.getInt("r"), so.getDouble("w").toFloat())
                            }
                        )
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readWeights(): List<WeightEntry> {
        val s = sp.getString("weights", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                WeightEntry(o.getLong("t"), o.getDouble("kg").toFloat(), o.optString("src", "manual"))
            }.sortedBy { it.time }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readWalks(): List<WalkLog> {
        val s = sp.getString("walks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                WalkLog(o.getLong("s"), o.getInt("d"), o.getInt("steps"), o.getDouble("m").toFloat(), o.optInt("kcal", 0))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readFavs(): List<String> {
        val s = sp.getString("favs", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readWater(): Map<String, Int> {
        val s = sp.getString("water", null) ?: return emptyMap()
        return try {
            val o = JSONObject(s)
            val m = HashMap<String, Int>()
            o.keys().forEach { k -> m[k] = o.getInt(k) }
            m
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun readActiveWalk(): ActiveWalk? {
        val s = sp.getString("activeWalk", null) ?: return null
        return try {
            val o = JSONObject(s)
            ActiveWalk(
                startedAt = o.getLong("startedAt"),
                running = o.getBoolean("running"),
                accumSteps = o.getInt("accumSteps"),
                accumMillis = o.getLong("accumMillis"),
                lastResumeAt = o.getLong("lastResumeAt"),
                baseline = o.getDouble("baseline").toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
}
