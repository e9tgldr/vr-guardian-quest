package mn.vrguardian.student

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.SpatialSDKExperimentalAPI
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpPerMeterDisplayOptions
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.createPanelEntity
import com.meta.spatial.vr.VRFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import mn.vrguardian.student.hub.HubSession
import mn.vrguardian.student.ui.StudentApp

/**
 * vr-guardian student app — a real Meta Spatial SDK app (not a web page).
 *
 * One spatial panel floats in the room; everything inside it is native Compose.
 * The hub session (poll + heartbeat) runs from here, so the teacher keeps seeing
 * this headset even if the panel is not being drawn.
 */
class MainActivity : AppSystemActivity() {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  override fun registerFeatures(): List<SpatialFeature> =
    mutableListOf<SpatialFeature>(VRFeature(this), ComposeFeature())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    HubSession.start(this, scope)
    Log.i(TAG, "onCreate: hub session started")
  }

  override fun onSceneReady() {
    super.onSceneReady()
    Log.i(TAG, "onSceneReady: building environment")

    scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
    scene.setLightingEnvironment(
      ambientColor = Vector3(0f),
      sunColor = Vector3(6.0f, 6.0f, 6.0f),
      sunDirection = -Vector3(1.0f, 3.0f, -2.0f),
      environmentIntensity = 0.3f,
    )
    runCatching { scene.updateIBLEnvironment("environment.env") }
      .onFailure { Log.w(TAG, "IBL environment missing: ${it.message}") }

    // the student stands 2 m back facing the panel, as in Meta's own samples
    scene.setViewOrigin(0.0f, 0.0f, 2.0f, 180.0f)

    // dark backdrop so the panel does not float in a black void
    Entity.create(
      listOf(
        Mesh(Uri.parse("mesh://skybox"), hittable = MeshCollision.NoCollision),
        Material().apply {
          baseTextureAndroidResourceId = R.drawable.skydome
          unlit = true
        },
        Transform(Pose(Vector3(0f, 0f, 0f))),
      ),
    )

    // panel ~1.7 m in front of the student at chest/eye height, facing them
    Entity.createPanelEntity(
      R.id.student_panel,
      Transform(Pose(Vector3(0f, PANEL_HEIGHT_M / 2f + 0.55f, PANEL_DISTANCE_Z), Quaternion(0f, 180f, 0f))),
    )
    Log.i(TAG, "onSceneReady: panel placed at z=$PANEL_DISTANCE_Z")
  }

  @OptIn(SpatialSDKExperimentalAPI::class)
  override fun registerPanels(): List<PanelRegistration> =
    listOf(
      ComposeViewPanelRegistration(
        R.id.student_panel,
        composeViewCreator = { _, context ->
          ComposeView(context).apply { setContent { StudentApp() } }
        },
        settingsCreator = {
          UIPanelSettings(
            shape = QuadShapeOptions(width = PANEL_WIDTH_M, height = PANEL_HEIGHT_M),
            style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
            display = DpPerMeterDisplayOptions(),
          )
        },
      ),
    )

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  companion object {
    private const val TAG = "VrGuardian"

    // 1.6 x 1.0 m ≈ an 800 x 500 dp window at the default 500 dp/m
    const val PANEL_WIDTH_M = 1.6f
    const val PANEL_HEIGHT_M = 1.0f

    /** viewer sits at z = +2 looking toward -Z, so this is ~1.7 m ahead of them. */
    const val PANEL_DISTANCE_Z = 0.3f
  }
}
