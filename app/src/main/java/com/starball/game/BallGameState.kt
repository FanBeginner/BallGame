package com.starball.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 游戏状态枚举
 */
enum class GameState {
    IDLE,           // 空闲状态
    FLYING_AWAY,    // 飞离状态
    FALLING_BACK,   // 掉落返回状态
    GAME_OVER       // 游戏结束
}

/**
 * 球的数据类
 */
data class Ball(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 80f,      // 初始半径
    var vx: Float = 0f,           // 水平速度
    var vy: Float = 0f,           // 垂直速度
    var scale: Float = 1.0f,      // 缩放比例（模拟近大远小）
    var rotation: Float = 0f,     // 旋转角度（度）
    var rotationSpeed: Float = 0f // 旋转速度（度/帧）
)

/**
 * 小鸡 NPC 数据类
 */
data class Chicken(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 100f,
    var height: Float = 100f,
    var isJumping: Boolean = false,
    var jumpOffset: Float = 0f    // 跳跃偏移
)

/**
 * 粒子数据类
 */
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Int,
    val alpha: Int = 255,
    val lifetime: Long = 800L,  // 粒子生命周期 (毫秒)
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAlive: Boolean
        get() = System.currentTimeMillis() - createdAt < lifetime

    val currentAlpha: Int
        get() {
            val elapsed = System.currentTimeMillis() - createdAt
            val progress = elapsed.toFloat() / lifetime
            return (alpha * (1 - progress)).toInt().coerceIn(0, 255)
        }
}

/**
 * 游戏状态数据类
 */
data class BallGameUiState(
    val state: GameState = GameState.IDLE,
    val ball: Ball = Ball(),
    val chicken: Chicken = Chicken(),
    val particles: List<Particle> = emptyList(),
    val score: Int = 0,
    val minY: Float = 0f,         // 球飞行的最高点判定线
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f
)
