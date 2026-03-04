package com.starball.game

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.sin
import kotlin.math.cos
import androidx.core.graphics.toColorInt

@Composable
fun BallGame(modifier: Modifier = Modifier) {
    val viewModel: BallGameViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val resources = context.resources

    // 加载资源
    val ballBitmap = remember {
        BitmapFactory.decodeResource(resources, R.drawable.ball)
    }
    val bgBitmap = remember {
        BitmapFactory.decodeResource(resources, R.drawable.bg1)
    }
    val chickenBitmap = remember {
        BitmapFactory.decodeResource(resources, R.drawable.chicken)
    }

    // 获取屏幕尺寸
    val density = LocalDensity.current
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }

    // 初始化游戏尺寸
    DisposableEffect(Unit) {
        // 在布局完成后初始化
        // 这里需要一个方式来获取实际尺寸
        onDispose { }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            val position = event.changes.first().position
                            viewModel.hitBall(position.x, position.y)
                        }
                    }
                }
            }
    ) {
        // 更新屏幕尺寸
        if (screenWidth != size.width || screenHeight != size.height) {
            screenWidth = size.width
            screenHeight = size.height
            viewModel.initializeGame(size.width.toInt(), size.height.toInt())
        }

        // 绘制背景
        drawBackground(bgBitmap, size.width, size.height)

        // 绘制阴影
        drawShadow(uiState)

        // 绘制小鸡 NPC
        drawChicken(uiState, chickenBitmap)

        // 绘制球
        drawBall(uiState, ballBitmap)

        // 绘制粒子效果
        drawParticles(uiState)

        // 绘制 UI
        drawUI(uiState)
    }
}

private fun DrawScope.drawBackground(bitmap: android.graphics.Bitmap, width: Float, height: Float) {
    drawImage(
        image = bitmap.asImageBitmap(),
        dstSize = IntSize(width.toInt(), height.toInt())
    )
}

private fun DrawScope.drawShadow(uiState: BallGameUiState) {
    val ball = uiState.ball
    val scale = ball.scale

    // 根据球的缩放（远近）动态计算影子的 Y 坐标和透明度
    val shadowYNear = uiState.screenHeight * 0.85f
    val shadowYFar = uiState.screenHeight * 0.5f

    // 使用 scale (范围 ~0.5 到 ~1.2) 作为插值因子
    val t = (1.0f - scale) * 2.0f // 当 scale=1, t=0; scale=0.5, t=1
    val shadowY = shadowYNear - t * (shadowYNear - shadowYFar)

    // 球越高（越远），影子越淡
    val alphaNear = 80
    val alphaFar = 20
    val shadowAlpha = ((alphaNear - t * (alphaNear - alphaFar)).coerceIn(0f, 255f)).toInt()

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.argb(shadowAlpha, 0, 0, 0)

    val shadowScale = scale * 0.8f

    drawContext.canvas.nativeCanvas.apply {
        save()
        translate(ball.x, shadowY)
        scale(shadowScale, 0.3f) // 压扁的阴影
        drawCircle(0f, 0f, ball.radius, paint)
        restore()
    }
}

private fun DrawScope.drawChicken(uiState: BallGameUiState, bitmap: android.graphics.Bitmap) {
    val chicken = uiState.chicken

    // 小鸡位置：固定在远方 minY 位置，加上跳跃偏移
    val drawY = chicken.y + chicken.jumpOffset

    val dstRect = RectF(
        chicken.x - chicken.width / 2,
        drawY - chicken.height / 2,
        chicken.x + chicken.width / 2,
        drawY + chicken.height / 2
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.alpha = 255

    drawContext.canvas.nativeCanvas.drawBitmap(bitmap, null, dstRect, paint)
}

private fun DrawScope.drawBall(uiState: BallGameUiState, bitmap: android.graphics.Bitmap) {
    val ball = uiState.ball
    val currentR = ball.radius * ball.scale

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.alpha = 255 // 确保球是不透明的

    // 如果是 FALLING_BACK 状态且 vy < 0，可以加一个光圈提示点击
    if (uiState.state == GameState.FALLING_BACK && ball.vy < 0) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = "#88FFFF00".toColorInt()
        drawContext.canvas.nativeCanvas.drawCircle(ball.x, ball.y, currentR + 10, paint)
        paint.style = Paint.Style.FILL
    }

    // IDLE 状态：绘制从球中心向外扩散的光波效果（3 个光圈循环）
    if (uiState.state == GameState.IDLE) {
        val time = System.currentTimeMillis()
        for (i in 0 until 3) {
            // 3 个光圈错开时间，每个间隔 500ms
            val waveTime = (time - i * 500) % 1500
            if (waveTime >= 0) {
                val waveProgress = waveTime / 1500f
                val waveRadius = currentR + waveProgress * currentR * 1.5f
                val waveAlpha = (255 * (1 - waveProgress)).toInt()

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 20f
                paint.color = Color.argb(waveAlpha, 100, 200, 255)
                drawContext.canvas.nativeCanvas.drawCircle(ball.x, ball.y, waveRadius, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    // 绘制旋转的球
    paint.alpha = 255
    drawContext.canvas.nativeCanvas.apply {
        save()
        translate(ball.x, ball.y)
        rotate(ball.rotation)
        val dstRect = RectF(-currentR, -currentR, currentR, currentR)
        drawBitmap(bitmap, null, dstRect, paint)
        restore()
    }
}

private fun DrawScope.drawUI(uiState: BallGameUiState) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE
    paint.textSize = 60f
    paint.textAlign = Paint.Align.CENTER

    drawContext.canvas.nativeCanvas.drawText(
        "SCORE: ${uiState.score}",
        uiState.screenWidth / 2f,
        150f,
        paint
    )

    if (uiState.state == GameState.GAME_OVER) {
        paint.color = "#CC0000".toColorInt()
        paint.textSize = 100f
        drawContext.canvas.nativeCanvas.drawText(
            "GAME OVER",
            uiState.screenWidth / 2f,
            uiState.screenHeight / 2f,
            paint
        )
        paint.textSize = 50f
        drawContext.canvas.nativeCanvas.drawText(
            "点击重来",
            uiState.screenWidth / 2f,
            uiState.screenHeight / 2f + 100,
            paint
        )
    }
}

private fun DrawScope.drawParticles(uiState: BallGameUiState) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    uiState.particles.forEach { particle ->
        paint.color = particle.color
        paint.alpha = particle.currentAlpha
        paint.style = Paint.Style.FILL

        drawContext.canvas.nativeCanvas.drawCircle(
            particle.x,
            particle.y,
            particle.radius,
            paint
        )
    }
}
