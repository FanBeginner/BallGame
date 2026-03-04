# Android 弹球游戏 - 实现总结

## 项目概述

基于 Jetpack Compose 开发的弹球游戏，使用 `Canvas` + `ViewModel` 架构实现。项目包含两个游戏：星星球（弹球游戏）和五子棋。

## 项目结构

```
app/src/main/java/com/starball/game/
├── MainActivity.kt          # 主 Activity，游戏选择菜单
├── BallGame.kt              # 星星球游戏 Compose 组件
├── BallGameViewModel.kt     # 星星球游戏 ViewModel
├── BallGameState.kt         # 数据类和状态定义
└── GomokuGame.kt            # 五子棋游戏组件
```

## 核心文件说明

### MainActivity.kt
- 主 Activity，设置全屏沉浸式体验
- `GameApp` 主导航组件，管理游戏选择状态
- `GameSelectionScreen` 游戏选择菜单（竖直排列的卡片）
- `BackHandler` 处理系统返回键，返回菜单页面

### BallGameState.kt
数据模型定义：

| 数据类 | 说明 |
|--------|------|
| `GameState` | 游戏状态枚举 (IDLE, FLYING_AWAY, FALLING_BACK, GAME_OVER) |
| `Ball` | 球的数据类 (位置、速度、缩放、旋转角度、旋转速度) |
| `Chicken` | 小鸡 NPC 数据类 (位置、尺寸、跳跃状态) |
| `Particle` | 粒子效果数据类 (位置、速度、颜色、生命周期) |
| `BallGameUiState` | 游戏 UI 状态总控 |

### BallGameViewModel.kt
游戏逻辑和状态管理：
- 使用 `StateFlow` 管理游戏状态
- 游戏循环：协程 + `delay(16)` 实现约 60 FPS
- 物理模拟：重力、速度、缩放（近大远小）、旋转
- 粒子效果生成和更新
- 碰撞检测和计分逻辑

### BallGame.kt
Compose 渲染组件：
- 使用 `Canvas` 进行自定义绘制
- `Modifier.pointerInput` 处理触摸事件
- 绘制层：背景 → 阴影 → 小鸡 → 球 → 粒子 → UI

## 游戏状态机

```
                    ┌─────────────────────────────────────┐
                    │                                     │
     ┌────────┐     │     ┌────────────┐                 │
     │  IDLE  │─────┴────►│ FLYING_AWAY │────────┐        │
     │ 空闲   │ 点击发射  │  飞离状态   │        │        │
     └───▲────┘         └──────┬───────┘        │        │
         │                     │                │        │
         │                     │ 达到最高点      │        │
         │                     ▼                │        │
         │             ┌──────────────┐         │        │
         └─────────────│ FALLING_BACK │◄────────┘        │
         点击反弹      │  掉落返回状态  │                  │
                       └──────┬───────┘                  │
                              │ 落出屏幕                  │
                              ▼                          │
                       ┌────────────┐                    │
                       │ GAME_OVER  │────────────────────┘
                       │  游戏结束   │  点击重来
                       └────────────┘
```

## 核心游戏逻辑

### 物理参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 重力加速度 | 1.2 | 每帧速度变化量 |
| 初始速度 | 45-55 | 击球时垂直初速度 |
| 水平速度 | ±5 | 随机水平偏移 |
| 帧率 | ~60 FPS | 游戏循环更新频率 |
| 旋转衰减 | 0.98 | 每帧旋转速度衰减系数 |

### 游戏规则

1. **IDLE 状态**：球在屏幕底部 75% 位置上下浮动（呼吸效果），显示光波动画
2. **发球**：首次点击为发球，不计分
3. **FLYING_AWAY 状态**：球向上飞行，缩放减小模拟远去，小鸡在远方跟随左右移动
4. **FALLING_BACK 状态**：球达到最高点后下落，缩放恢复模拟靠近，小鸡跳跃一次
5. **GAME_OVER 状态**：球落出屏幕底部，显示游戏结束

### 计分规则

- 首次击球为发球，不计分
- 后续每次成功击球（在 IDLE 或 FALLING_BACK 状态点击球）得 1 分
- 游戏结束后重置分数

## 视觉效果

### 球的旋转
- 根据点击位置偏离球心的比例计算初始旋转速度
- 点击左侧产生逆时针旋转，点击右侧产生顺时针旋转
- 旋转速度每帧衰减 2%
- 绘制时使用 `canvas.rotate()` 实现

### 粒子效果
- 击球得分时在击球位置生成 20 个粒子
- 颜色：金色 (#FFD700)、黄色 (#FFFF00)、橙色 (#FFA500)、白色 (#FFFFFF)
- 呈放射状飞散，受重力影响下落
- 生命周期 800ms，透明度逐渐降低

### 光波效果 (IDLE 状态)
- 3 个光圈从球心向外扩散
- 光圈颜色：淡蓝色 (RGB: 100, 200, 255)
- 光圈间距 500ms，持续 1.5 秒循环
- 透明度随扩散逐渐降低

### 提示光圈 (FALLING_BACK 状态)
- 球下落且速度向上时显示黄色光圈
- 提示玩家可以点击击球

### 阴影效果
- 根据球的缩放（远近）动态计算影子 Y 坐标和透明度
- 远处影子更淡，近处影子更深

### 小鸡 NPC
- 位置固定在远方 `minY` 处（屏幕高度 40% 位置）
- FLYING_AWAY 状态：跟随球左右移动
- FALLING_BACK 状态：跳跃一次模拟拍球（600ms 动画）
- 跳跃时显示黄色光圈

## 交互逻辑

```kotlin
// 触摸事件处理
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

// 碰撞检测
val dx = x - ball.x
val dy = y - ball.y
val distance = sqrt(dx * dx + dy * dy)
val hitRadius = ball.radius * ball.scale * 1.5f
if (distance < hitRadius) {
    // 成功击中球
}
```

## 技术架构

### ViewModel + StateFlow
```kotlin
class BallGameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BallGameUiState())
    val uiState: StateFlow<BallGameUiState> = _uiState.asStateFlow()

    // 游戏循环
    private fun startGameLoop() {
        viewModelScope.launch {
            while (gameLoopActive) {
                updatePhysics()
                delay(16) // 约 60 FPS
            }
        }
    }
}
```

### Compose 组件
```kotlin
@Composable
fun BallGame(modifier: Modifier = Modifier) {
    val viewModel: BallGameViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Canvas(modifier = modifier.fillMaxSize()) {
        // 绘制游戏内容
    }
}
```

## 资源文件

| 资源名 | 说明 |
|--------|------|
| `R.drawable.ball` | 球的图片 |
| `R.drawable.bg1` | 背景图片 |
| `R.drawable.chicken` | 小鸡 NPC 图片 |

## 游戏选择菜单

- 竖直排列的两个游戏卡片
- 星星球（蓝色卡片）：点击球挑战高分
- 五子棋（绿色卡片）：经典策略对战
- 点击卡片进入游戏，按返回键返回菜单

## 待测试项目

1. [ ] 启动后显示游戏选择菜单
2. [ ] 点击星星球进入游戏
3. [ ] 球在底部上下浮动，显示光波效果
4. [ ] 首次点击发球，球旋转飞出
5. [ ] 球达到最高点后下落，小鸡跳跃
6. [ ] 下落过程中点击球可反弹得分
7. [ ] 得分时显示粒子效果
8. [ ] 球落出屏幕显示游戏结束
9. [ ] 点击"点击重来"重新开始
10. [ ] 返回键返回菜单

## 注意事项

1. 游戏循环使用 `delay(16)` 控制约 60FPS
2. 使用 `collectAsState()` 收集 StateFlow 状态
3. 屏幕尺寸通过 Canvas 的 `size` 属性动态获取
4. 触摸检测使用圆形碰撞公式
5. 旋转效果通过 `canvas.rotate()` 实现
6. 粒子效果使用列表管理，自动清理死亡粒子
