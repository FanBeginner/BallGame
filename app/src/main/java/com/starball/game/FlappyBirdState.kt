package com.starball.game

/**
 * 游戏状态枚举
 */
enum class FlappyBirdState {
    READY,      // 准备状态（等待点击开始）
    PLAYING,    // 游戏进行中
    GAME_OVER   // 游戏结束
}

/**
 * 小鸟数据类
 */
data class Bird(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 85f,
    var height: Float = 60f,
    var velocity: Float = 0f,       // 垂直速度
    var rotation: Float = 0f,       // 旋转角度（度）
    var hoverOffset: Float = 0f,    // 准备状态的浮动偏移
    var frameIndex: Int = 0,        // 动画帧索引（0, 1, 2）
    var animating: Boolean = false, // 是否正在播放动画
    var animationFrame: Int = 0     // 动画帧计数器
)

/**
 * 管道数据类
 */
data class Pipe(
    var x: Float = 0f,
    var width: Float = 120f,
    var gapY: Float = 0f,          // 间隙的 Y 坐标（上管道底部）
    var gapHeight: Float = 180f,   // 间隙高度
    var speed: Float = 4f,
    var passed: Boolean = false    // 是否已通过（计分用）
)

/**
 * 背景层数据类
 */
data class BackgroundLayer(
    var x: Float = 0f,
    var speed: Float = 2f
)

/**
 * 分数数据类
 */
data class GameScore(
    val current: Int = 0,
    val highScore: Int = 0
)

/**
 * 笨小鸟游戏 UI 状态
 */
data class FlappyBirdUiState(
    val state: FlappyBirdState = FlappyBirdState.READY,
    val bird: Bird = Bird(),
    val pipes: List<Pipe> = emptyList(),
    val score: GameScore = GameScore(),
    val house: BackgroundLayer = BackgroundLayer(speed = 2f),
    val tree: BackgroundLayer = BackgroundLayer(speed = 3f),
    val floor: BackgroundLayer = BackgroundLayer(speed = 4f),
    val frameCount: Int = 0,
    val screenWidth: Float = 0f,
    val screenHeight: Float = 0f
)