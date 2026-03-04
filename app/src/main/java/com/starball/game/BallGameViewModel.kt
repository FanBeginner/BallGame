package com.starball.game

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random
import androidx.core.graphics.toColorInt

/**
 * BallGame ViewModel
 * 管理游戏状态和物理逻辑
 */
class BallGameViewModel : ViewModel() {

    private val random = Random(System.currentTimeMillis())

    // 重力加速度
    private val gravity = 1.2f

    // UI 状态
    private val _uiState = MutableStateFlow(BallGameUiState())
    val uiState: StateFlow<BallGameUiState> = _uiState.asStateFlow()

    // 游戏循环是否活跃
    private var gameLoopActive = false

    // 小鸡跳跃动画时间戳
    private var chickenJumpStartTime = 0L
    private var hasChickenJumped = false

    // 首次击球标记（发球不计分）
    private var isFirstHit = true

    /**
     * 生成粒子效果
     */
    private fun generateParticles(x: Float, y: Float): List<Particle> {
        val particles = mutableListOf<Particle>()
        val particleCount = 20  // 粒子数量

        for (i in 0 until particleCount) {
            val angle = (i.toFloat() / particleCount) * 360f
            val speed = 5f + random.nextFloat() * 10f
            val vx = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * speed
            val vy = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * speed
            val radius = 5f + random.nextFloat() * 10f

            // 金色和黄色系粒子
            val colors = listOf(
                "#FFD700".toColorInt(),  // 金色
                "#FFFF00".toColorInt(),  // 黄色
                "#FFA500".toColorInt(),  // 橙色
                "#FFFFFF".toColorInt()   // 白色
            )
            val color = colors[random.nextInt(colors.size)]

            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    radius = radius,
                    color = color,
                    alpha = 255,
                    lifetime = 800L
                )
            )
        }
        return particles
    }

    /**
     * 更新粒子状态
     */
    private fun updateParticles(particles: List<Particle>): List<Particle> {
        return particles.mapNotNull { particle ->
            if (!particle.isAlive) {
                null  // 移除死亡粒子
            } else {
                particle.copy(
                    x = particle.x + particle.vx,
                    y = particle.y + particle.vy,
                    vy = particle.vy + 0.5f  // 重力影响
                )
            }
        }
    }

    init {
        startGameLoop()
    }

    /**
     * 初始化游戏尺寸
     */
    fun initializeGame(width: Int, height: Int) {
        _uiState.update { state ->
            state.copy(
                screenWidth = width.toFloat(),
                screenHeight = height.toFloat(),
                minY = height * 0.4f,
                ball = state.ball.copy(
                    x = width / 2f,
                    y = height * 0.75f,
                    scale = 1.0f,
                    vx = 0f,
                    vy = 0f
                ),
                chicken = state.chicken.copy(
                    x = width / 2f,
                    y = state.minY,  // 小鸡在远方 minY 位置
                    width = 200f,
                    height = 200f,
                    isJumping = false,
                    jumpOffset = 0f
                ),
                score = 0  // 初始化时重置分数
            )
        }
        // 重置首次击球标记
        isFirstHit = true
    }

    /**
     * 重置球到初始位置
     */
    fun resetBall() {
        val state = _uiState.value
        _uiState.update { it.copy(
            state = GameState.IDLE,
            ball = it.ball.copy(
                x = it.screenWidth / 2f,
                y = it.screenHeight * 0.75f,
                scale = 1.0f,
                vx = 0f,
                vy = 0f,
                rotation = 0f,
                rotationSpeed = 0f
            ),
            chicken = it.chicken.copy(
                x = it.screenWidth / 2f,
                isJumping = false,
                jumpOffset = 0f
            )
        )}
        // 重置小鸡跳跃状态
        hasChickenJumped = false
        chickenJumpStartTime = 0L
        // 重置首次击球标记
        isFirstHit = false // 游戏结束后重置，下次击球算分
    }

    /**
     * 点击球
     */
    fun hitBall(x: Float, y: Float) {
        val state = _uiState.value

        // 游戏结束状态下点击，重置游戏
        if (state.state == GameState.GAME_OVER) {
            _uiState.update { it.copy(score = 0) }
            resetBall()
            isFirstHit = true // 新游戏开始，首次击球为发球
            return
        }

        // 只有在 IDLE 或 FALLING_BACK 状态可以击球
        if (state.state != GameState.IDLE && state.state != GameState.FALLING_BACK) {
            return
        }

        // 点击碰撞检测
        val dx = x - state.ball.x
        val dy = y - state.ball.y
        val distance = sqrt(dx * dx + dy * dy)
        val hitRadius = state.ball.radius * state.ball.scale * 1.5f

        if (distance < hitRadius) {
            // 进入飞行状态
            val shouldAddScore = !isFirstHit // 首次击球（发球）不计分
            val newScore = if (shouldAddScore) state.score + 1 else state.score

            // 如果得分，生成粒子效果
            val newParticles = if (shouldAddScore) {
                generateParticles(x, y)
            } else {
                emptyList()
            }

            // 根据点击位置计算旋转方向和速度（偏离中心越远，旋转越快）
            val offsetRatio = (dx / hitRadius).coerceIn(-1f, 1f)
            val initialRotationSpeed = offsetRatio * 15f

            _uiState.update {
                it.copy(
                    state = GameState.FLYING_AWAY,
                    score = newScore,
                    particles = newParticles,
                    ball = it.ball.copy(
                        vy = (45 + random.nextInt(10)).toFloat(),
                        vx = if(it.ball.x > it.screenWidth/2) -(random.nextFloat() * 5) else random.nextFloat() * 5,
                        rotationSpeed = initialRotationSpeed
                    )
                )
            }
            // 首次击球后，标记为已发球
            if (state.state == GameState.IDLE) {
                isFirstHit = false
            }
        }
    }

    /**
     * 更新物理逻辑
     */
    private fun updatePhysics() {
        val state = _uiState.value
        val ball = state.ball
        val chicken = state.chicken

        val updatedBall = when (state.state) {
            GameState.IDLE -> {
                // 呼吸效果：球微弱上下浮动
                val breathY = state.screenHeight * 0.75f +
                    kotlin.math.sin(System.currentTimeMillis() / 200.0).toFloat() * 20f
                ball.copy(y = breathY)
            }

            GameState.FLYING_AWAY -> {
                // 往远方飞：向上移动，vy 减小，scale 减小
                var newVy = ball.vy - gravity
                var newVx = ball.vx
                val newY = ball.y - newVy
                var newX = ball.x + ball.vx
                var newScale = ball.scale
                var newRotation = ball.rotation + ball.rotationSpeed
                var newRotationSpeed = ball.rotationSpeed

                // 左右边界限制
                val currentR = ball.radius * newScale
                newX = newX.coerceIn(currentR, state.screenWidth - currentR)

                // 模拟远去：缩放比例向 0.5 靠拢
                if (newScale > 0.5f) newScale -= 0.01f

                // 旋转速度逐渐衰减
                newRotationSpeed *= 0.98f

                // 当速度转为向下，且位置超过了"远端判定线"时，进入 FALLING_BACK
                if (newVy < 0 && newY > state.minY) {
                    _uiState.update { it.copy(state = GameState.FALLING_BACK) }
                    newVy = (20 + random.nextInt(10)).toFloat()
                    newVx = if(ball.x > uiState.value.screenWidth/2) -(random.nextFloat() * 5) else random.nextFloat() * 5
                    // 触发小鸡跳跃
                    chickenJumpStartTime = System.currentTimeMillis()
                    hasChickenJumped = true
                }

                ball.copy(x = newX, y = newY, vy = newVy, vx = newVx, scale = newScale, rotation = newRotation, rotationSpeed = newRotationSpeed)
            }

            GameState.FALLING_BACK -> {
                // 往回掉：向下移动，vy 继续受重力增加，scale 增加
                val newVy = ball.vy - gravity
                val newY = ball.y - newVy
                var newX = ball.x + ball.vx
                var newScale = ball.scale
                var newRotation = ball.rotation + ball.rotationSpeed
                var newRotationSpeed = ball.rotationSpeed

                // 左右边界限制
                val currentR = ball.radius * newScale
                newX = newX.coerceIn(currentR, state.screenWidth - currentR)

                // 模拟飞回：缩放比例向 1.2 恢复
                if (newScale < 1.2f) newScale += 0.015f

                // 旋转速度逐渐衰减
                newRotationSpeed *= 0.98f

                // 如果掉出屏幕下方：游戏结束
                if (newY > state.screenHeight + 200) {
                    _uiState.update { it.copy(state = GameState.GAME_OVER) }
                }

                ball.copy(x = newX, y = newY, vy = newVy, scale = newScale, rotation = newRotation, rotationSpeed = newRotationSpeed)
            }

            GameState.GAME_OVER -> ball
        }

        // 更新小鸡状态
        val updatedChicken = when (state.state) {
            GameState.FLYING_AWAY -> {
                // 球向上移动时，小鸡在远方跟随球左右移动
                chicken.copy(
                    x = updatedBall.x,
                    y = state.minY,  // 固定在远方位置
                    isJumping = false,
                    jumpOffset = 0f
                )
            }

            GameState.FALLING_BACK -> {
                // 球下落时，小鸡跳跃模拟拍球（只跳一次）
                val jumpOffset = if (hasChickenJumped) {
                    val elapsed = System.currentTimeMillis() - chickenJumpStartTime
                    if (elapsed < 600) {
                        // 跳跃动画持续 600ms
                        val jumpProgress = elapsed / 600f
                        -kotlin.math.sin(jumpProgress * kotlin.math.PI).toFloat() * 80f
                    } else {
                        0f
                    }
                } else {
                    0f
                }
                chicken.copy(
                    x = updatedBall.x,
                    y = state.minY,  // 固定在远方位置
                    isJumping = jumpOffset < 0,
                    jumpOffset = jumpOffset
                )
            }

            else -> {
                // IDLE 和 GAME_OVER 状态，小鸡在远方静止
                chicken.copy(
                    x = updatedBall.x,
                    y = state.minY,  // 固定在远方位置
                    isJumping = false,
                    jumpOffset = 0f
                )
            }
        }

        // 更新粒子状态
        val updatedParticles = updateParticles(state.particles)

        _uiState.update { it.copy(ball = updatedBall, chicken = updatedChicken, particles = updatedParticles) }
    }

    /**
     * 启动游戏循环
     */
    private fun startGameLoop() {
        gameLoopActive = true
        viewModelScope.launch {
            while (gameLoopActive) {
                updatePhysics()
                delay(16) // 约 60 FPS
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopActive = false
    }
}
