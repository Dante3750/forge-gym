package app.forge.gym.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ExerciseRepo {

    private fun JSONObject.str(key: String, fallback: String): String =
        if (isNull(key)) fallback else optString(key, fallback)

    private fun JSONObject.strList(key: String): List<String> {
        val arr: JSONArray = optJSONArray(key) ?: return emptyList()
        return List(arr.length()) { arr.getString(it) }
    }

    fun load(context: Context): List<Exercise> {
        val text = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<Exercise>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Exercise(
                id = o.getString("id"),
                name = o.getString("name"),
                level = o.str("level", "beginner"),
                equipment = o.str("equipment", "none"),
                category = o.str("category", "strength"),
                mechanic = o.str("mechanic", ""),
                force = o.str("force", ""),
                primary = o.strList("primaryMuscles"),
                secondary = o.strList("secondaryMuscles"),
                steps = o.strList("instructions"),
                images = o.strList("images")
            )
        }
        return out
    }
}
