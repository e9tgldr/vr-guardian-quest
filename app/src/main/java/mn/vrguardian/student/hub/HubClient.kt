package mn.vrguardian.student.hub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/** App / search-topic catalogue — mirrors serve_app.py on the teacher laptop. */
object Catalogue {
  const val CALC = "calc"
  const val NOTES = "notes"
  const val BROWSER = "browser"
  const val GAME = "game"

  val appIds = listOf(CALC, NOTES, BROWSER, GAME)
  val appNames = mapOf(
    CALC to "Калькулятор",
    NOTES to "Тэмдэглэл",
    BROWSER to "Интернет",
    GAME to "Тоглоом",
  )
  val appHints = mapOf(
    CALC to "Математик хичээлийн апп",
    NOTES to "Хичээлийн тэмдэглэл",
    BROWSER to "Хяналттай хөтөч",
    GAME to "Хичээлийн үед хориглосон",
  )

  const val TOPIC_MATH = "math"
  const val TOPIC_HISTORY = "history"
  const val TOPIC_GAMEDL = "gamedl"
  const val TOPIC_TIKTOK = "tiktok"

  val topicIds = listOf(TOPIC_MATH, TOPIC_HISTORY, TOPIC_GAMEDL, TOPIC_TIKTOK)
  val topicNames = mapOf(
    TOPIC_MATH to "математик дасгал",
    TOPIC_HISTORY to "Монгол түүх",
    TOPIC_GAMEDL to "тоглоом татах",
    TOPIC_TIKTOK to "TikTok",
  )

  fun appName(id: String?): String = appNames[id] ?: (id ?: "—")
}

/** What the teacher laptop reports. */
data class HubState(
  val studentsOnline: Int = 0,
  val blockedApps: Set<String> = setOf(Catalogue.GAME),
  val blockedTopics: Set<String> = emptySet(),
)

/** Thin JSON client for the vr-guardian hub (serve_app.py). No third-party deps. */
class HubClient(var baseUrl: String, var sid: String, var name: String) {

  private fun open(path: String, method: String): HttpURLConnection {
    val conn = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
    conn.requestMethod = method
    conn.connectTimeout = 2500
    conn.readTimeout = 3500
    conn.setRequestProperty("Accept", "application/json")
    return conn
  }

  private fun readAll(conn: HttpURLConnection): String {
    val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
    val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
    conn.disconnect()
    return text
  }

  private fun strings(json: JSONObject, key: String): Set<String> = try {
    val arr: JSONArray = json.optJSONArray(key) ?: JSONArray()
    (0 until arr.length()).map { arr.getString(it) }.toSet()
  } catch (t: Throwable) {
    emptySet()
  }

  /** GET /api/state — null when the hub is unreachable. */
  fun fetchState(): HubState? = try {
    val json = JSONObject(readAll(open("/api/state", "GET")))
    HubState(
      studentsOnline = json.optInt("studentsOnline", 0),
      blockedApps = strings(json, "blockedApps"),
      blockedTopics = strings(json, "blockedTopics"),
    )
  } catch (t: Throwable) {
    null
  }

  /** POST /api/student — heartbeat plus the app the student has open. */
  fun heartbeat(app: String?, search: String? = null): Boolean = try {
    val body = JSONObject().apply {
      put("sid", sid)
      put("app", app ?: JSONObject.NULL)
      if (search != null) put("search", search)
      if (name.isNotBlank()) put("name", name)
    }.toString().toByteArray(Charsets.UTF_8)

    val conn = open("/api/student", "POST").apply {
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=utf-8")
      setRequestProperty("Content-Length", body.size.toString())
    }
    conn.outputStream.use { it.write(body) }
    readAll(conn)
    true
  } catch (t: Throwable) {
    false
  }
}

/** Local identity + settings, persisted across launches. */
class Prefs(context: Context) {
  private val sp = context.getSharedPreferences("vrguardian", Context.MODE_PRIVATE)

  var serverUrl: String
    get() = sp.getString("serverUrl", "http://127.0.0.1:8000") ?: "http://127.0.0.1:8000"
    set(value) {
      sp.edit().putString("serverUrl", value.trim()).apply()
    }

  var studentName: String
    get() = sp.getString("studentName", "") ?: ""
    set(value) {
      sp.edit().putString("studentName", value.trim()).apply()
    }

  /** Stable per-install id; the hub shows it as "Сурагч N" until a name is set. */
  val sid: String
    get() {
      val existing = sp.getString("sid", null)
      if (!existing.isNullOrBlank()) return existing
      val generated = "q" + (1..4).map { "abcdefghijkmnpqrstuvwxyz23456789"[Random.nextInt(32)] }.joinToString("")
      sp.edit().putString("sid", generated).apply()
      return generated
    }
}
