package com.starball.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
/**
 * 五子棋游戏
 * */
private const val BOARD_SIZE = 15

private enum class Player {
    BLACK, WHITE
}

// Represents the state of the game
private class GomokuState {
    var board by mutableStateOf(Array(BOARD_SIZE) { Array<Player?>(BOARD_SIZE) { null } })
    var currentPlayer by mutableStateOf(Player.BLACK)
    var winner by mutableStateOf<Player?>(null)
    val isGameOver: Boolean
        get() = winner != null || isBoardFull()

    private val moveHistory = mutableStateListOf<Pair<Int, Int>>()
    val canUndo: Boolean get() = moveHistory.isNotEmpty() && !isGameOver

    fun makeMove(row: Int, col: Int) {
        if (isGameOver || board[row][col] != null) return

        moveHistory.add(row to col)

        val newBoard = board.map { it.clone() }.toTypedArray()
        newBoard[row][col] = currentPlayer
        board = newBoard

        if (checkForWin(row, col)) {
            winner = currentPlayer
        } else {
            currentPlayer = if (currentPlayer == Player.BLACK) Player.WHITE else Player.BLACK
        }
    }

    fun undo() {
        if (!canUndo) return

        val (row, col) = moveHistory.removeLast()

        val newBoard = board.map { it.clone() }.toTypedArray()
        newBoard[row][col] = null
        board = newBoard

        currentPlayer = if (currentPlayer == Player.BLACK) Player.WHITE else Player.BLACK
        winner = null // A win might have been undone
    }

    private fun checkForWin(row: Int, col: Int): Boolean {
        val player = board[row][col] ?: return false
        // Check horizontal, vertical, and both diagonals
        return checkLine(player, row, col, 1, 0) || // Horizontal
               checkLine(player, row, col, 0, 1) || // Vertical
               checkLine(player, row, col, 1, 1) || // Diagonal (down-right)
               checkLine(player, row, col, 1, -1)   // Diagonal (up-right)
    }

    private fun checkLine(player: Player, row: Int, col: Int, dRow: Int, dCol: Int): Boolean {
        var count = 1
        // Check in the positive direction
        for (i in 1 until 5) {
            val r = row + i * dRow
            val c = col + i * dCol
            if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == player) {
                count++
            } else {
                break
            }
        }
        // Check in the negative direction
        for (i in 1 until 5) {
            val r = row - i * dRow
            val c = col - i * dCol
            if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board[r][c] == player) {
                count++
            } else {
                break
            }
        }
        return count >= 5
    }

    private fun isBoardFull(): Boolean = board.all { row -> row.all { it != null } }

    fun reset() {
        board = Array(BOARD_SIZE) { Array<Player?>(BOARD_SIZE) { null } }
        currentPlayer = Player.BLACK
        winner = null
        moveHistory.clear()
    }
}

@Composable
fun GomokuGame(onBack: () -> Unit = {}) {
    val state = remember { GomokuState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0EAD6)), // Beige background
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextButton(onClick = onBack) {
                androidx.compose.material3.Text("返回")
            }
            GameStatus(state)
        }
        GomokuBoard(state)
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { state.reset() }) {
                Text("Restart Game")
            }
            Button(onClick = { state.undo() }, enabled = state.canUndo) {
                Text("Undo")
            }
        }
    }
}

@Composable
private fun GameStatus(state: GomokuState) {
    val text = when {
        state.winner != null -> "${state.winner} Wins!"
        state.isGameOver -> "It's a Draw!"
        else -> "Turn: ${state.currentPlayer}"
    }
    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp),
        color = if (state.winner == Player.BLACK) Color.Black else if (state.winner == Player.WHITE) Color.White else Color.Gray
    )
}

@Composable
private fun GomokuBoard(state: GomokuState) {
    var potentialMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(1f)
            .background(Color(0xFFD2B48C)) // Wooden color for the board
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (!state.isGameOver) {
                            val cellSize = size.width / (BOARD_SIZE - 1)
                            val col = (offset.x / cellSize).roundToInt()
                            val row = (offset.y / cellSize).roundToInt()

                            if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE && state.board[row][col] == null) {
                                potentialMove = row to col
                                val released = tryAwaitRelease()
                                if (released) {
                                    state.makeMove(row, col)
                                }
                                potentialMove = null
                            }
                        }
                    }
                )
            }
        ) {
            drawBoard()
            drawPieces(state.board, potentialMove, state.currentPlayer)
        }
    }
}

private fun DrawScope.drawBoard() {
    val cellSize = size.width / (BOARD_SIZE - 1)
    // Draw grid lines
    for (i in 0 until BOARD_SIZE) {
        val pos = i * cellSize
        // Vertical lines
        drawLine(Color.Black, start = Offset(pos, 0f), end = Offset(pos, size.height))
        // Horizontal lines
        drawLine(Color.Black, start = Offset(0f, pos), end = Offset(size.width, pos))
    }
}

private fun DrawScope.drawPieces(
    board: Array<Array<Player?>>,
    potentialMove: Pair<Int, Int>?,
    currentPlayer: Player
) {
    val cellSize = size.width / (BOARD_SIZE - 1)
    val pieceRadius = cellSize / 2.5f

    // Draw existing pieces
    board.forEachIndexed { row, cells ->
        cells.forEachIndexed { col, player ->
            if (player != null) {
                val center = Offset(col * cellSize, row * cellSize)
                val color = if (player == Player.BLACK) Color.Black else Color.White
                drawCircle(color, radius = pieceRadius, center = center)
            }
        }
    }

    // Draw ghost piece for potential move
    potentialMove?.let { (row, col) ->
        val center = Offset(col * cellSize, row * cellSize)
        val color = (if (currentPlayer == Player.BLACK) Color.Black else Color.White).copy(alpha = 0.5f)
        drawCircle(color, radius = pieceRadius, center = center)
    }
}

@Preview(showBackground = true)
@Composable
fun GomokuGamePreview() {
    MaterialTheme {
        GomokuGame()
    }
}
