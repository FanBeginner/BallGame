# Android Compose 弹球游戏 - 实现文档

## 项目概述

基于 Jetpack Compose 开发的弹球游戏，将 HTML5 Canvas 弹球游戏移植到 Android 平台。

**严格按照原始 HTML5 游戏逻辑实现，无墙壁碰撞。**

## 项目结构

```
app/src/main/java/com/starball/game/
├── MainActivity.kt              # 主 Activity
├── data/
│   └── GameRepository.kt        # 数据持久化（DataStore）
├── logic/
│   ├── GameState.kt             # 游戏状态机
│   ├── PhysicsEngine.kt         # 物理引擎
│   └── CollisionDetector.kt     # 碰撞检测器
├── model/
│   ├── Ball.kt                  # 球数据模型
│   ├── Chicken.kt               # 小鸡数据模型
│   └── GameStats.kt             # 游戏统计数据
├── ui/
│   ├── components/
│   │   ├── BackgroundLayer.kt   # 背景层（天空、星星、地面）
│   │   ├── BallRenderer.kt      # 球绘制组件
│   │   ├── ChickenRenderer.kt   # 小鸡绘制组件
│   │   ├── ScoreBoard.kt        # 记分板
│   │   └── GameOverPanel.kt     # 游戏结束面板
│   ├── screens/
│   │   ├── GameScreen.kt        # 游戏主屏幕
│   │   └── GameViewModel.kt     # 游戏 ViewModel
│   └── theme/
│       ├── GameColors.kt        # 游戏颜色主题
│       └── GameTypography.kt    # 排版主题
```

## 核心功能

### 1. 游戏状态机

```kotlin
sealed class GameState {
    object Idle : GameState()        // 待机：小球上下浮动
    object PlayerLaunch : GameState() // 玩家发球：球向上飞出
    object Returning : GameState()   // 返回中：球触底反弹后向上
    object GameOver : GameState()    // 游戏结束
}
```

### 2. 游戏流程图

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  ┌──────┐    点击球     ┌─────────────┐   触底   ┌─────────────┐
│  │ Idle │ ────────────> │ PlayerLaunch│ ─────────> │ Returning │
│  └──────┘               └─────────────┘           └─────────────┘
│      ^                                                  │
│      │                                                  │ 点击球
│      │ 点击球（接球）                                   │
│      └──────────────────────────────────────────────────┘
│
│      ┌─────────────┐
│      │  GameOver   │ <── 球飞出屏幕底部（未接住）
│      └─────────────┘
```

### 3. 物理引擎参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 重力加速度 | 0.5 | 每帧 Y 轴速度增加量 |
| 发球初速度 | 30 | 玩家发球时的初始速度 |
| 反弹速度 | 28 | 球触底反弹的速度 |
| 最大横向速度 | 12 | X 轴方向最大速度 |
| 发射角度范围 | PI/3.2 ~ PI/2.5 | 约 56° ~ 72° |

### 4. 游戏规则（严格按照原始 HTML5 游戏）

1. **待机状态**：小球在底部地面上方上下浮动（正弦波）
2. **玩家发球**：点击小球，球以 56°-72° 角度向上飞出，方向随机
3. **重力下落**：球受重力影响（g=0.5），速度逐渐向下
4. **触底反弹**：球触底后自动反弹向上（速度 28）
5. **玩家接球**：在球运动过程中点击球，球再次向上飞出，得分 +1
6. **游戏结束**：球飞出屏幕底部（y 坐标超过屏幕高度）

**注意：没有墙壁碰撞，球可以自由飞出左右屏幕**

### 5. 难度递增

- 分数越高，发球速度越快（`launchSpeed + score * 0.5`）
- 分数越高，触底反弹角度越随机
- 分数 >= 10 时，小球发光

## 技术实现

### Jetpack Compose

- `Canvas`：自定义绘制游戏元素
- `pointerInput` + `detectTapGestures`：触摸事件处理
- `LaunchedEffect`：屏幕尺寸初始化
- `collectAsState`：UI 响应式更新

### ViewModel + StateFlow

- `MutableStateFlow`：游戏状态管理
- `AndroidViewModel`：获取 Application Context
- `viewModelScope`：协程作用域

### DataStore

- 持久化保存最高分
- 记录游戏次数
- 使用 `Preferences` 存储

## 视觉效果

### 背景层
- 渐变夜空（深蓝到浅蓝）
- 50 颗随机星星（固定种子）
- 绿色渐变地面
- 3 朵装饰云朵

### 小球
- 径向渐变（模拟 3D）
- 高光效果
- 影子跟随
- 缩放效果（模拟远近：y 越小越远，越小）
- 发光效果（高分时）

### 小鸡
- 身体（渐变黄色）
- 鸡冠（红色渐变）
- 眼睛（带高光）
- 嘴巴（橙色）
- 腮红（粉色）
- 翅膀（深黄色）
- 脚（深橙色）
- 三种状态：待机、接球、庆祝

## 游戏操作

1. **待机状态**：小球在底部上下浮动
2. **发球**：点击小球，球以抛物线轨迹飞出
3. **接球**：在球运动过程中点击小球可再次发球
4. **得分**：每次成功接球 +1 分
5. **游戏结束**：球飞出屏幕底部前未点击到球

## 计分系统

- 每次成功接球 +1 分
- 分数越高，球速越快
- 分数 > 10 时，小球发光
- 最高分自动保存到 DataStore
- 游戏结束显示是否创造新纪录

## 性能优化

- 使用 `remember` 缓存计算结果
- 游戏循环使用协程延迟控制帧率（~60fps）
- 避免在 Composable 中创建新对象
- 使用 `StateFlow` 进行响应式更新

## 适配说明

- 自动适配不同屏幕尺寸
- 使用 `LocalConfiguration` 获取屏幕参数
- 地面高度为屏幕高度的 10%
- 小鸡和球的位置根据屏幕尺寸动态计算

## 编译说明

使用 Android Studio 打开项目，确保已安装：
- Android Gradle Plugin 8.3.0
- Kotlin 1.9.10
- Jetpack Compose

运行命令：
```bash
./gradlew assembleDebug
```

## 许可证

MIT License
