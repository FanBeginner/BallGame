package com.starball.game

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.PI

@Composable
fun FlappyBirdGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: FlappyBirdViewModel = viewModel {
        FlappyBirdViewModel(context.applicationContext)
    }
    val uiState by viewModel.uiState.collectAsState()

    // 加载资源
    val birdBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.bird)
    }
    // 上管道图片（pipe1）
    val pipeTopBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.pipe1)
    }
    // 下管道图片（pipe0）
    val pipeBottomBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.pipe0)
    }

    // 获取屏幕尺寸
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            viewModel.onTap()
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

        // 绘制天空背景
        drawBackground()

        // 绘制视差滚动背景
        drawBackgroundLayers(uiState)

        // 绘制管道
        drawPipes(uiState, pipeTopBitmap, pipeBottomBitmap)

        // 绘制地板
        drawFloor(uiState)

        // 绘制小鸟
        drawBird(uiState, birdBitmap)

        // 绘制 UI（分数、提示）
        drawUI(uiState)
    }
}

private fun DrawScope.drawBackground() {
    drawRect(Color(0xFF87CEEB)) // 天空蓝
}

private fun DrawScope.drawBackgroundLayers(uiState: FlappyBirdUiState) {
    // 使用纯色绘制视差背景（可替换为图片）
    val floorY = uiState.screenHeight - 48f

    // 房子层（最远，速度 2）
    val houseX = uiState.house.x
    val houseWidth = uiState.screenWidth
    drawRect(
        Color(0xFFB0BEC5),
        topLeft = Offset(houseX, floorY - 200f),
        size = Size(houseWidth, 200f)
    )
    drawRect(
        Color(0xFFB0BEC5),
        topLeft = Offset(houseX + houseWidth, floorY - 200f),
        size = Size(houseWidth, 200f)
    )

    // 树层（中间，速度 3）
    val treeX = uiState.tree.x
    val treeWidth = uiState.screenWidth
    drawRect(
        Color(0xFF81C784),
        topLeft = Offset(treeX, floorY - 150f),
        size = Size(treeWidth, 150f)
    )
    drawRect(
        Color(0xFF81C784),
        topLeft = Offset(treeX + treeWidth, floorY - 150f),
        size = Size(treeWidth, 150f)
    )
}

private fun DrawScope.drawPipes(uiState: FlappyBirdUiState, pipeTopBitmap: android.graphics.Bitmap?, pipeBottomBitmap: android.graphics.Bitmap?) {
    val floorY = uiState.screenHeight - 48f

    uiState.pipes.forEach { pipe ->
        // 上管道使用 pipe0
        if (pipeTopBitmap != null) {
            val pipeRect = RectF(pipe.x, 0f, pipe.x + pipe.width, pipe.gapY)
            drawContext.canvas.nativeCanvas.drawBitmap(pipeTopBitmap, null, pipeRect, null)
        } else {
            // 备用：绿色管道
            val pipeColor = Color(0xFF4CAF50)
            drawRect(
                pipeColor,
                topLeft = Offset(pipe.x, 0f),
                size = Size(pipe.width, pipe.gapY)
            )
            drawRect(
                Color(0xFF388E3C),
                topLeft = Offset(pipe.x - 5f, pipe.gapY - 20f),
                size = Size(pipe.width + 10f, 20f)
            )
        }

        // 下管道使用 pipe1
        if (pipeBottomBitmap != null) {
            val bottomPipeRect = RectF(
                pipe.x,
                pipe.gapY + pipe.gapHeight,
                pipe.x + pipe.width,
                floorY
            )
            drawContext.canvas.nativeCanvas.drawBitmap(pipeBottomBitmap, null, bottomPipeRect, null)
        } else {
            // 备用：绿色管道
            val pipeColor = Color(0xFF4CAF50)
            drawRect(
                pipeColor,
                topLeft = Offset(pipe.x, pipe.gapY + pipe.gapHeight),
                size = Size(pipe.width, floorY - (pipe.gapY + pipe.gapHeight))
            )
            drawRect(
                Color(0xFF388E3C),
                topLeft = Offset(pipe.x - 5f, pipe.gapY + pipe.gapHeight),
                size = Size(pipe.width + 10f, 20f)
            )
        }
    }
}

private fun DrawScope.drawFloor(uiState: FlappyBirdUiState) {
    val floorY = uiState.screenHeight - 48f

    // 地板层
    val floorX = uiState.floor.x
    val floorWidth = uiState.screenWidth

    drawRect(Color(0xFFDEB887), Offset(floorX, floorY), size = Size(floorWidth, 48f))
    drawRect(Color(0xFFDEB887), Offset(floorX + floorWidth, floorY), size = Size(floorWidth, 48f))

    // 地板纹理线条
    val lineColor = AndroidColor.parseColor("#D2691E")
    val paint = Paint()
    paint.color = lineColor
    paint.strokeWidth = 2f

    drawContext.canvas.nativeCanvas.apply {
        val segmentWidth = 40f
        val startX = floorX % segmentWidth
        for (i in 0..((floorWidth * 2 / segmentWidth).toInt() + 1)) {
            val x = startX + i * segmentWidth
            // 斜线纹理
            drawLine(x, floorY, x - 20f, floorY + 48f, paint)
        }
    }
}

private fun DrawScope.drawBird(uiState: FlappyBirdUiState, birdBitmap: android.graphics.Bitmap?) {
    val bird = uiState.bird
    val drawY = bird.y + (if (uiState.state == FlappyBirdState.READY) bird.hoverOffset else 0f)

    if (birdBitmap != null) {
        // 使用小鸟图片（三帧动画）
        // 计算每帧的宽度（图片总宽度 / 3）
        val frameWidth = birdBitmap.width / 3
        val frameHeight = birdBitmap.height
        val frameIndex = bird.frameIndex

        // 源矩形：从三帧图片中裁剪对应帧
        val srcRect = Rect(
            frameIndex * frameWidth,
            0,
            (frameIndex + 1) * frameWidth,
            frameHeight
        )

        // 目标矩形：绘制位置
        val dstRect = RectF(bird.x, drawY, bird.x + bird.width, drawY + bird.height)

        drawContext.canvas.nativeCanvas.apply {
            save()
            translate(bird.x + bird.width / 2, drawY + bird.height / 2)
            rotate((bird.rotation * PI / 180f).toFloat())
            translate(-(bird.x + bird.width / 2), -(drawY + bird.height / 2))
            drawBitmap(birdBitmap, srcRect, dstRect, null)
            restore()
        }
    }
}

private fun DrawScope.drawUI(uiState: FlappyBirdUiState) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.WHITE
    paint.textSize = 48f
    paint.textAlign = Paint.Align.CENTER

    // 分数
    drawContext.canvas.nativeCanvas.drawText(
        "${uiState.score.current}",
        uiState.screenWidth / 2f,
        100f,
        paint
    )

    // 最高分
    paint.textSize = 24f
    paint.alpha = 180
    drawContext.canvas.nativeCanvas.drawText(
        "最高分: ${uiState.score.highScore}",
        uiState.screenWidth / 2f,
        140f,
        paint
    )

    when (uiState.state) {
        FlappyBirdState.READY -> {
            paint.textSize = 36f
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            paint.color = AndroidColor.WHITE
            paint.setShadowLayer(4f, 2f, 2f, AndroidColor.BLACK)
            drawContext.canvas.nativeCanvas.drawText(
                "点击屏幕开始",
                uiState.screenWidth / 2f,
                uiState.screenHeight / 2f + 100f,
                paint
            )
            paint.setShadowLayer(0f, 0f, 0f, AndroidColor.TRANSPARENT)
        }
        FlappyBirdState.GAME_OVER -> {
            paint.textSize = 60f
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            paint.color = AndroidColor.parseColor("#CC0000")
            paint.setShadowLayer(4f, 2f, 2f, AndroidColor.BLACK)
            drawContext.canvas.nativeCanvas.drawText(
                "游戏结束",
                uiState.screenWidth / 2f,
                uiState.screenHeight / 2f,
                paint
            )
            paint.textSize = 36f
            paint.color = AndroidColor.WHITE
            drawContext.canvas.nativeCanvas.drawText(
                "点击重新开始",
                uiState.screenWidth / 2f,
                uiState.screenHeight / 2f + 80f,
                paint
            )
            paint.setShadowLayer(0f, 0f, 0f, AndroidColor.TRANSPARENT)
        }
        else -> {}
    }
}