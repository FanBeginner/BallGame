package com.starball.game

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

enum class GameType(val title: String, val description: String, val color: Color) {
    BALL_GAME("星星球", "点击球挑战高分", Color(0xFF64B5F6)),
    GOMOKU("五子棋", "经典策略对战", Color(0xFF81C784)),
    FLAPPY_BIRD("笨小鸟", "经典闯关挑战", Color(0xFFFFD54F))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏状态栏和导航栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameApp()
                }
            }
        }
    }
}

@Composable
fun GameApp() {
    var currentGame by remember { mutableStateOf<GameType?>(null) }

    when (val game = currentGame) {
        null -> GameSelectionScreen(onGameSelected = { currentGame = it })
        GameType.BALL_GAME -> BallGameScreen(onBack = { currentGame = null })
        GameType.GOMOKU -> GomokuGameScreen(onBack = { currentGame = null })
        GameType.FLAPPY_BIRD -> FlappyBirdScreen(onBack = { currentGame = null })
    }
}

@Composable
fun GameSelectionScreen(onGameSelected: (GameType) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "选择游戏",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                GameType.values().forEach { gameType ->
                    GameCard(
                        gameType = gameType,
                        onClick = { onGameSelected(gameType) }
                    )
                }
            }
        }
    }
}

@Composable
fun GameCard(gameType: GameType, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = gameType.color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = gameType.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = gameType.description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BallGameScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    BallGame(modifier = Modifier.fillMaxSize())
}

@Composable
fun GomokuGameScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    GomokuGame(onBack = onBack)
}

@Composable
fun FlappyBirdScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    FlappyBirdGame(modifier = Modifier.fillMaxSize())
}
