package com.starball.game

import android.content.Context
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

/**
 * 笨小鸟游戏 ViewModel
 * 管理游戏状态和物理逻辑
 */
class FlappyBirdViewModel(
    private val context: Context
) : ViewModel() {

    // 物理常量（适配网页版参数）
    private val gravity = 0.5f              // 重力加速度
    private val jumpVelocity = -10f         // 跳跃初速度
    private val pipeSpawnInterval = 85      // 管道生成间隔（帧）
    private val pipeSpeed = 4f              // 管道移动速度
    private val groundHeight = 48f          // 地板高度
    private val floorSpeed = 4f

    private val _uiState = MutableStateFlow(FlappyBirdUiState())
    val uiState: StateFlow<FlappyBirdUiState> = _uiState.asStateFlow()

    private var gameLoopActive = false

    init {
        startGameLoop()
        loadHighScore()
    }

    /**
     * 启动游戏循环
     */
    private fun startGameLoop() {
        gameLoopActive = true
        viewModelScope.launch {
            while (gameLoopActive) {
                updateGame()
                delay(16) // 约 60 FPS
            }
        }
    }

    /**
     * 初始化游戏尺寸
     */
    fun initializeGame(width: Int, height: Int) {
        // 只在尺寸变化时更新
        val current = _uiState.value
        if (current.screenWidth == width.toFloat() && current.screenHeight == height.toFloat()) {
            return
        }

        _uiState.update { state ->
            state.copy(
                screenWidth = width.toFloat(),
                screenHeight = height.toFloat(),
                bird = Bird(
                    x = width * 0.3f,
                    y = height * 0.5f,
                    width = 60f * (width / 1080f).coerceAtLeast(0.8f),
                    height = 42f * (width / 1080f).coerceAtLeast(0.8f)
                ),
                score = GameScore(current = 0, highScore = state.score.highScore)
            )
        }
    }

    /**
     * 处理点击事件
     */
    fun onTap() {
        val state = _uiState.value
        when (state.state) {
            FlappyBirdState.READY -> {
                _uiState.update { it.copy(state = FlappyBirdState.PLAYING) }
            }
            FlappyBirdState.PLAYING -> {
                _uiState.update {
                    it.copy(bird = it.bird.copy(
                        velocity = jumpVelocity,
                        rotation = -25f,
                        animating = true,
                        animationFrame = 0,
                        frameIndex = 0
                    ))
                }
            }
            FlappyBirdState.GAME_OVER -> {
                viewModelScope.launch {
                    delay(300)
                    resetGame()
                }
            }
        }
    }

    /**
     * 更新游戏状态
     */
    private fun updateGame() {
        val state = _uiState.value

        when (state.state) {
            FlappyBirdState.READY -> {
                // 准备状态：小鸟上下浮动并扇动翅膀
                val hoverOffset = kotlin.math.sin(System.currentTimeMillis() / 300.0).toFloat() * 20f
                // 准备状态持续播放动画帧（循环 0, 1, 2）
                val frameIndex = ((System.currentTimeMillis() / 150) % 3).toInt()
                _uiState.update { it.copy(bird = it.bird.copy(hoverOffset = hoverOffset, frameIndex = frameIndex)) }
            }
            FlappyBirdState.PLAYING -> {
                // 更新小鸟物理
                val bird = updateBirdPhysics(state.bird)

                // 碰撞检测
                if (checkCollisions(bird, state)) {
                    saveHighScore()
                    _uiState.update {
                        it.copy(
                            state = FlappyBirdState.GAME_OVER,
                            bird = bird
                        )
                    }
                    return
                }

                // 更新管道
                var pipes = state.pipes.map { pipe ->
                    pipe.copy(x = pipe.x - pipe.speed)
                }.filter { it.x + it.width > 0 }

                // 生成新管道
                if (state.frameCount % pipeSpawnInterval == 0) {
                    pipes = pipes + createNewPipe(state.screenWidth, state.screenHeight)
                }

                // 更新分数
                var score = state.score.current
                pipes = pipes.map { pipe ->
                    if (!pipe.passed && pipe.x + pipe.width < bird.x) {
                        score++
                        pipe.copy(passed = true)
                    } else {
                        pipe
                    }
                }

                // 更新背景层
                val newHouse = updateBackgroundLayer(state.house, state.screenWidth, 2f)
                val newTree = updateBackgroundLayer(state.tree, state.screenWidth, 3f)
                val newFloor = updateBackgroundLayer(state.floor, state.screenWidth, floorSpeed)

                val highScore = max(state.score.highScore, score)

                _uiState.update {
                    it.copy(
                        bird = bird,
                        pipes = pipes,
                        score = GameScore(score, highScore),
                        house = newHouse,
                        tree = newTree,
                        floor = newFloor,
                        frameCount = it.frameCount + 1
                    )
                }
            }
            FlappyBirdState.GAME_OVER -> {
                // 小鸟继续下落
                val bird = state.bird.copy(
                    velocity = state.bird.velocity + gravity,
                    y = state.bird.y + state.bird.velocity,
                    rotation = 90f.coerceAtMost(state.bird.rotation + 5f)
                )
                _uiState.update { it.copy(bird = bird) }
            }
        }
    }

    /**
     * 更新小鸟物理状态
     */
    private fun updateBirdPhysics(bird: Bird): Bird {
        val newVelocity = bird.velocity + gravity
        val newY = bird.y + newVelocity

        // 旋转角度根据速度调整
        val targetRotation = when {
            newVelocity < 0 -> -25f
            newVelocity < 3 -> 0f
            else -> 90f
        }
        val newRotation = (bird.rotation * 0.9f + targetRotation * 0.1f).coerceIn(-25f, 90f)

        // 更新动画帧
        var newFrameIndex = bird.frameIndex
        var newAnimating = bird.animating
        var newAnimationFrame = bird.animationFrame

        if (bird.animating) {
            newAnimationFrame++
            // 每 3 帧切换一次动画帧
            if (newAnimationFrame % 3 == 0) {
                newFrameIndex = (newFrameIndex + 1) % 3
            }
            // 动画播放 9 帧后停止（3帧循环一次）
            if (newAnimationFrame >= 9) {
                newAnimating = false
                newFrameIndex = 0
            }
        }

        return bird.copy(
            velocity = newVelocity,
            y = newY,
            rotation = newRotation,
            frameIndex = newFrameIndex,
            animating = newAnimating,
            animationFrame = newAnimationFrame
        )
    }

    /**
     * 碰撞检测
     */
    private fun checkCollisions(bird: Bird, state: FlappyBirdUiState): Boolean {
        // 地面碰撞
        if (bird.y + bird.height > state.screenHeight - groundHeight) {
            return true
        }

        // 天花板碰撞
        if (bird.y < 0) {
            return true
        }

        // 管道碰撞（使用 AABB 碰撞检测，缩小碰撞盒提高容错）
        val padding = bird.width * 0.1f
        val birdRect = RectF(
            bird.x + padding,
            bird.y + padding,
            bird.x + bird.width - padding,
            bird.y + bird.height - padding
        )

        for (pipe in state.pipes) {
            // 上管道
            val topPipeRect = RectF(pipe.x, 0f, pipe.x + pipe.width, pipe.gapY)
            // 下管道
            val bottomPipeRect = RectF(
                pipe.x,
                pipe.gapY + pipe.gapHeight,
                pipe.x + pipe.width,
                state.screenHeight - groundHeight
            )

            if (RectF.intersects(birdRect, topPipeRect) ||
                RectF.intersects(birdRect, bottomPipeRect)) {
                return true
            }
        }

        return false
    }

    /**
     * 创建新管道
     */
    private fun createNewPipe(screenWidth: Float, screenHeight: Float): Pipe {
        val pipeWidth = screenWidth * 0.12f
        val gapHeight = screenHeight * 0.25f
        val minGapY = screenHeight * 0.2f
        val maxGapY = screenHeight - groundHeight - gapHeight - minGapY
        val gapY = Random.nextFloat() * (maxGapY - minGapY) + minGapY

        return Pipe(
            x = screenWidth,
            width = pipeWidth,
            gapY = gapY,
            gapHeight = gapHeight,
            speed = pipeSpeed
        )
    }

    /**
     * 更新背景层位置（实现视差滚动）
     */
    private fun updateBackgroundLayer(layer: BackgroundLayer, screenWidth: Float, speed: Float): BackgroundLayer {
        var newX = layer.x - speed
        if (newX < -screenWidth) {
            newX = 0f
        }
        return layer.copy(x = newX, speed = speed)
    }

    /**
     * 重置游戏
     */
    private fun resetGame() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                state = FlappyBirdState.READY,
                bird = Bird(
                    x = state.screenWidth * 0.3f,
                    y = state.screenHeight * 0.5f,
                    width = it.bird.width,
                    height = it.bird.height,
                    velocity = 0f,
                    rotation = 0f
                ),
                pipes = emptyList(),
                score = GameScore(0, it.score.highScore),
                frameCount = 0
            )
        }
    }

    /**
     * 加载最高分
     */
    private fun loadHighScore() {
        val prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE)
        val highScore = prefs.getInt("high_score", 0)
        _uiState.update { it.copy(score = GameScore(current = 0, highScore = highScore)) }
    }

    /**
     * 保存最高分
     */
    private fun saveHighScore() {
        val state = _uiState.value
        val prefs = context.getSharedPreferences("FlappyBirdPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("high_score", state.score.highScore).apply()
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopActive = false
    }
}