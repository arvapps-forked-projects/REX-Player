package xyz.mpv.rex.ui.player.ambient

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import xyz.mpv.rex.ui.player.PlayerViewModel
import xyz.mpv.rex.ui.player.VideoAspect

@Composable
fun AmbientGlowOverlay(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier
) {
  val isEnabled by viewModel.ambientController.isAmbientEnabled.collectAsState()
  if (!isEnabled) return

  val aspectMode by viewModel.videoAspect.collectAsState()
  val customAspect by viewModel.currentAspectRatio.collectAsState()
  val zoom by viewModel.videoZoom.collectAsState()
  val advancedZoom by viewModel.advancedZoomEnabled.collectAsState()

  // Ambient glow is designed strictly for letterbox/pillarbox margins in Fit mode without zoom (same as YouTube)
  if (aspectMode != VideoAspect.Fit || customAspect > 0 || zoom != 0f || advancedZoom) {
    return
  }

  val ambientColors by viewModel.ambientController.ambientColors.collectAsState()

  val animLeft by animateColorAsState(
    targetValue = ambientColors.left,
    animationSpec = tween(400),
    label = "ambient_left"
  )
  val animRight by animateColorAsState(
    targetValue = ambientColors.right,
    animationSpec = tween(400),
    label = "ambient_right"
  )
  val animTop by animateColorAsState(
    targetValue = ambientColors.top,
    animationSpec = tween(400),
    label = "ambient_top"
  )
  val animBottom by animateColorAsState(
    targetValue = ambientColors.bottom,
    animationSpec = tween(400),
    label = "ambient_bottom"
  )

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val screenW = constraints.maxWidth.toFloat()
    val screenH = constraints.maxHeight.toFloat()
    if (screenW <= 0f || screenH <= 0f) return@BoxWithConstraints

    val screenAr = screenW / screenH
    val rawVidAr = viewModel.getVideoOutAspect() ?: (16f / 9f)
    val vidAr = rawVidAr.toFloat()

    // Margins calculation: either pillarbox OR letterbox, NEVER both at once
    val leftBarWidth: Float
    val rightBarStart: Float
    val topBarHeight: Float
    val bottomBarStart: Float

    if (screenAr > vidAr) {
      // Pillarbox: black bars on left and right
      val videoW = screenH * vidAr
      leftBarWidth = ((screenW - videoW) / 2f).coerceAtLeast(0f)
      rightBarStart = (screenW - leftBarWidth).coerceAtMost(screenW)
      topBarHeight = 0f
      bottomBarStart = screenH
    } else {
      // Letterbox: black bars on top and bottom
      val videoH = screenW / vidAr
      topBarHeight = ((screenH - videoH) / 2f).coerceAtLeast(0f)
      bottomBarStart = (screenH - topBarHeight).coerceAtMost(screenH)
      leftBarWidth = 0f
      rightBarStart = screenW
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
      // Left bar ambient glow
      if (leftBarWidth > 1f && animLeft != Color.Transparent) {
        val leftBrush = Brush.horizontalGradient(
          colors = listOf(Color.Transparent, animLeft.copy(alpha = 0.55f)),
          startX = 0f,
          endX = leftBarWidth
        )
        drawRect(
          brush = leftBrush,
          topLeft = Offset.Zero,
          size = Size(leftBarWidth, screenH)
        )
      }

      // Right bar ambient glow
      if (rightBarStart < screenW - 1f && animRight != Color.Transparent) {
        val rightBrush = Brush.horizontalGradient(
          colors = listOf(animRight.copy(alpha = 0.55f), Color.Transparent),
          startX = rightBarStart,
          endX = screenW
        )
        drawRect(
          brush = rightBrush,
          topLeft = Offset(rightBarStart, 0f),
          size = Size(screenW - rightBarStart, screenH)
        )
      }

      // Top bar ambient glow
      if (topBarHeight > 1f && animTop != Color.Transparent) {
        val topBrush = Brush.verticalGradient(
          colors = listOf(Color.Transparent, animTop.copy(alpha = 0.55f)),
          startY = 0f,
          endY = topBarHeight
        )
        drawRect(
          brush = topBrush,
          topLeft = Offset.Zero,
          size = Size(screenW, topBarHeight)
        )
      }

      // Bottom bar ambient glow
      if (bottomBarStart < screenH - 1f && animBottom != Color.Transparent) {
        val bottomBrush = Brush.verticalGradient(
          colors = listOf(animBottom.copy(alpha = 0.55f), Color.Transparent),
          startY = bottomBarStart,
          endY = screenH
        )
        drawRect(
          brush = bottomBrush,
          topLeft = Offset(0f, bottomBarStart),
          size = Size(screenW, screenH - bottomBarStart)
        )
      }
    }
  }
}
