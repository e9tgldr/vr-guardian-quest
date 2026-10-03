package mn.vrguardian.student.hub

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App-wide hub session.
 *
 * The poll loop lives here (started by the Activity), NOT inside the Compose panel:
 * a supervision client must keep reporting to the teacher even when the panel is not
 * being rendered, and must still enforce a block the moment it arrives.
 */
object HubSession {
  private const val TAG = "VrGuardian"

  var state by mutableStateOf(HubState())
    private set
  var online by mutableStateOf(false)
    private set
  var currentApp by mutableStateOf<String?>(null)
    private set

  /** Bumped when a block is enforced remotely, so the UI can react. */
  var blockedEventAt by mutableStateOf(0L)
    private set

  private var client: HubClient? = null
  private var started = false

  val sid: String get() = client?.sid ?: "—"

  fun start(context: Context, scope: CoroutineScope) {
    if (started) return
    started = true
    val prefs = Prefs(context.applicationContext)
    val hub = HubClient(prefs.serverUrl, prefs.sid, prefs.studentName)
    client = hub
    Log.i(TAG, "hub session start: url=${hub.baseUrl} sid=${hub.sid} name='${hub.name}'")

    scope.launch {
      var ticks = 0
      while (true) {
        val fresh = hub.fetchState()
        online = fresh != null
        if (fresh != null) {
          state = fresh
          if (ticks % 8 == 0) {
            Log.i(TAG, "hub online: blockedApps=${fresh.blockedApps} blockedTopics=${fresh.blockedTopics}")
          }
          val open = currentApp
          if (open != null && fresh.blockedApps.contains(open)) {
            Log.i(TAG, "remote block: closing '$open'")
            currentApp = null
            blockedEventAt = System.currentTimeMillis()
          }
        } else if (ticks % 8 == 0) {
          Log.w(TAG, "hub unreachable at ${hub.baseUrl}")
        }
        hub.heartbeat(currentApp)
        ticks++
        delay(1200)
      }
    }
  }

  fun setLocalIdentity(url: String, name: String) {
    client?.let {
      it.baseUrl = url
      it.name = name
    }
    Log.i(TAG, "settings applied: url=$url name='$name'")
  }

  fun openApp(id: String) {
    currentApp = id
    client?.heartbeat(id)
    Log.i(TAG, "open app: $id")
  }

  fun closeApp() {
    currentApp = null
    client?.heartbeat(null)
    Log.i(TAG, "close app")
  }

  fun reportSearch(topicId: String, topicLabel: String) {
    client?.heartbeat(currentApp, topicId)
    Log.i(TAG, "search: $topicLabel")
  }

  fun isAppBlocked(id: String) = state.blockedApps.contains(id)

  fun isTopicBlocked(id: String) = state.blockedTopics.contains(id)
}
