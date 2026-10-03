package mn.vrguardian.student.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mn.vrguardian.student.hub.Catalogue
import mn.vrguardian.student.hub.HubSession
import mn.vrguardian.student.hub.Prefs

object Palette {
  val bg = Color(0xFF0B0E15)
  val card = Color(0xFF152036)
  val cardBlocked = Color(0xFF1A1F2B)
  val stroke = Color(0xFF243352)
  val text = Color(0xFFDBE4FF)
  val dim = Color(0xFF8FA0C0)
  val accent = Color(0xFF4CC3FF)
  val green = Color(0xFF22E08A)
  val red = Color(0xFFFF5A5A)
  val yellow = Color(0xFFFFD34D)
  val violet = Color(0xFF9B8CFF)
}

private enum class Screen { LAUNCHER, APP, SEARCH, SETTINGS }

/**
 * The whole student experience. Deliberately unbranded: a plain learning
 * launcher. vr-guardian only becomes visible at the moment of restriction.
 */
@Composable
fun StudentApp() {
  val context = LocalContext.current
  val prefs = remember { Prefs(context.applicationContext) }

  var screen by remember { mutableStateOf(Screen.LAUNCHER) }
  var bannerTitle by remember { mutableStateOf<String?>(null) }
  var bannerSub by remember { mutableStateOf("") }
  var lastSearchLabel by remember { mutableStateOf<String?>(null) }
  var lastSearchBlocked by remember { mutableStateOf(false) }

  val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90) }.getOrNull() }
  DisposableEffect(Unit) {
    onDispose { runCatching { tone?.release() } }
  }

  fun denied() {
    runCatching { tone?.startTone(ToneGenerator.TONE_PROP_NACK, 260) }
  }

  fun showBanner(title: String, sub: String) {
    bannerTitle = title
    bannerSub = sub
  }

  // a block enforced by the poll loop (which runs even when this panel is not drawn)
  LaunchedEffect(HubSession.blockedEventAt) {
    val stamp = HubSession.blockedEventAt
    if (stamp == 0L) return@LaunchedEffect
    denied()
    showBanner("БАГШ ТҮГЖЛЭЭ", "Апп хаагдлаа")
    screen = Screen.LAUNCHER
  }

  LaunchedEffect(bannerTitle) {
    if (bannerTitle != null) {
      delay(2400)
      bannerTitle = null
    }
  }

  val state = HubSession.state
  val online = HubSession.online
  val currentApp = HubSession.currentApp

  fun tapApp(id: String) {
    if (HubSession.isAppBlocked(id)) {
      denied()
      showBanner("Багшийн зөвшөөрөл шаардлагатай", Catalogue.appName(id) + " апп түгжигдсэн байна")
      return
    }
    HubSession.openApp(id)
    screen = if (id == Catalogue.BROWSER) Screen.SEARCH else Screen.APP
  }

  fun runSearch(topicId: String) {
    val blocked = HubSession.isTopicBlocked(topicId)
    lastSearchLabel = Catalogue.topicNames[topicId]
    lastSearchBlocked = blocked
    if (blocked) {
      denied()
      showBanner("Багш зөвшөөрөхгүй!", Catalogue.topicNames[topicId] + " хайлт хориглосон")
    }
    HubSession.reportSearch(topicId, Catalogue.topicNames[topicId] ?: topicId)
  }

  MaterialTheme(colorScheme = darkColorScheme(background = Palette.bg, surface = Palette.card, primary = Palette.accent)) {
    Box(Modifier.fillMaxSize().background(Palette.bg)) {
      Column(Modifier.fillMaxSize().padding(18.dp)) {
        Header(online = online, studentsOnline = state.studentsOnline, sid = HubSession.sid) { screen = Screen.SETTINGS }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
          when (screen) {
            Screen.LAUNCHER -> Launcher(state.blockedApps) { tapApp(it) }
            Screen.SEARCH -> SearchScreen(
              blockedTopics = state.blockedTopics,
              lastLabel = lastSearchLabel,
              lastBlocked = lastSearchBlocked,
              onSearch = { runSearch(it) },
              onBack = {
                HubSession.closeApp()
                screen = Screen.LAUNCHER
              },
            )
            Screen.APP -> AppContent(currentApp) {
              HubSession.closeApp()
              screen = Screen.LAUNCHER
            }
            Screen.SETTINGS -> SettingsScreen(prefs) {
              screen = if (currentApp == null) Screen.LAUNCHER else Screen.APP
            }
          }
        }
      }
      bannerTitle?.let { Banner(it, bannerSub) }
    }
  }
}

@Composable
private fun Header(online: Boolean, studentsOnline: Int, sid: String, onSettings: () -> Unit) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text("Миний сургалт", color = Palette.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
      Text(
        if (online) "Холбогдсон · " + studentsOnline + " төхөөрөмж" else "Холболт хүлээж байна",
        color = if (online) Palette.dim else Palette.red,
        fontSize = 13.sp,
      )
    }
    Box(Modifier.size(11.dp).background(if (online) Palette.green else Palette.red, RoundedCornerShape(50)))
    Spacer(Modifier.width(10.dp))
    Text("Тохиргоо", color = Palette.dim, fontSize = 13.sp, modifier = Modifier.clickable { onSettings() })
    Spacer(Modifier.width(10.dp))
    Text(sid, color = Palette.stroke, fontSize = 12.sp)
  }
}

@Composable
private fun Launcher(blockedApps: Set<String>, onTap: (String) -> Unit) {
  val ids = Catalogue.appIds
  val accents = listOf(Palette.accent, Palette.violet, Palette.green, Palette.yellow)
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    for (row in 0 until 2) {
      Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (col in 0 until 2) {
          val index = row * 2 + col
          val id = ids[index]
          Tile(
            name = Catalogue.appName(id),
            hint = Catalogue.appHints[id] ?: "",
            blocked = blockedApps.contains(id),
            accent = accents[index],
            modifier = Modifier.weight(1f).fillMaxSize(),
            onClick = { onTap(id) },
          )
        }
      }
    }
  }
}

@Composable
private fun Tile(
  name: String,
  hint: String,
  blocked: Boolean,
  accent: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val shape = RoundedCornerShape(16.dp)
  Column(
    modifier
      .background(if (blocked) Palette.cardBlocked else Palette.card, shape)
      .border(1.dp, if (blocked) Palette.stroke else accent.copy(alpha = 0.55f), shape)
      .clickable { onClick() }
      .padding(18.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(name, color = if (blocked) Palette.dim else Palette.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(
      if (blocked) "хориглосон" else "идэвхтэй",
      color = if (blocked) Palette.red else Palette.green,
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))
    Text(hint, color = Palette.dim, fontSize = 12.sp)
  }
}

@Composable
private fun SearchScreen(
  blockedTopics: Set<String>,
  lastLabel: String?,
  lastBlocked: Boolean,
  onSearch: (String) -> Unit,
  onBack: () -> Unit,
) {
  val ids = Catalogue.topicIds
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("Хайлт", color = Palette.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      BackButton(onBack)
    }
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      for (col in 0 until 2) {
        val id = ids[col]
        Chip(Catalogue.topicNames[id] ?: id, blockedTopics.contains(id), Modifier.weight(1f).fillMaxSize()) { onSearch(id) }
      }
    }
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      for (col in 2 until 4) {
        val id = ids[col]
        Chip(Catalogue.topicNames[id] ?: id, blockedTopics.contains(id), Modifier.weight(1f).fillMaxSize()) { onSearch(id) }
      }
    }
    Text(
      when {
        lastLabel == null -> "Сурагчийн хайлт энд харагдана (багш шууд харна)."
        lastBlocked -> "Хориглосон хайлт: " + lastLabel
        else -> "Хайлтын үр дүн: " + lastLabel
      },
      color = if (lastBlocked) Palette.red else Palette.dim,
      fontSize = 14.sp,
      fontWeight = if (lastBlocked) FontWeight.Bold else FontWeight.Normal,
    )
  }
}

@Composable
private fun Chip(label: String, blocked: Boolean, modifier: Modifier, onClick: () -> Unit) {
  val shape = RoundedCornerShape(14.dp)
  Box(
    modifier
      .background(if (blocked) Palette.cardBlocked else Palette.card, shape)
      .border(1.dp, if (blocked) Palette.red.copy(alpha = 0.6f) else Palette.stroke, shape)
      .clickable { onClick() }
      .padding(14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, color = if (blocked) Palette.dim else Palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(6.dp))
      Text(if (blocked) "хориглосон" else "хайх", color = if (blocked) Palette.red else Palette.green, fontSize = 13.sp)
    }
  }
}

@Composable
private fun AppContent(appId: String?, onBack: () -> Unit) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(Catalogue.appName(appId), color = Palette.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      BackButton(onBack)
    }
    Box(
      Modifier.fillMaxWidth().weight(1f).background(Palette.card, RoundedCornerShape(16.dp))
        .border(1.dp, Palette.stroke, RoundedCornerShape(16.dp)).padding(20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (appId) {
          Catalogue.CALC -> {
            Text("12 × 8 = 96", color = Palette.text, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Демо тооцоолуур", color = Palette.dim, fontSize = 14.sp)
          }
          Catalogue.NOTES -> {
            Text("Хичээлийн тэмдэглэл", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("1. Дасгал 12 — бодлого 3, 4\n2. Гэрийн даалгавар: 21-р хуудас", color = Palette.dim, fontSize = 15.sp)
          }
          else -> {
            Text("Тоглоом", color = Palette.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Оноо: 1 240 · Түвшин 4", color = Palette.yellow, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text("Багш түгжихэд энэ апп шууд хаагдана.", color = Palette.dim, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
  Button(
    onClick = onBack,
    colors = ButtonDefaults.buttonColors(containerColor = Palette.card, contentColor = Palette.text),
  ) { Text("Буцах", fontSize = 14.sp) }
}

@Composable
private fun SettingsScreen(prefs: Prefs, onClose: () -> Unit) {
  var url by remember { mutableStateOf(prefs.serverUrl) }
  var name by remember { mutableStateOf(prefs.studentName) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("Тохиргоо", color = Palette.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      BackButton(onClose)
    }
    OutlinedTextField(
      value = url,
      onValueChange = { url = it },
      label = { Text("Багшийн сервер (URL)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = name,
      onValueChange = { name = it },
      label = { Text("Сурагчийн нэр (сонголтоор)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Text(
      "USB холболт: http://127.0.0.1:8000  ·  Wi-Fi: http://<лаптопын-IP>:8000",
      color = Palette.dim,
      fontSize = 13.sp,
    )
    Button(
      onClick = {
        prefs.serverUrl = url
        prefs.studentName = name
        HubSession.setLocalIdentity(prefs.serverUrl, prefs.studentName)
        onClose()
      },
    ) { Text("Хадгалах") }
  }
}

@Composable
private fun Banner(title: String, sub: String) {
  Box(
    Modifier.fillMaxSize().background(Palette.red.copy(alpha = 0.92f)),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(10.dp))
      Text(sub, color = Color.White, fontSize = 16.sp)
    }
  }
}
