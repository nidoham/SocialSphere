package com.nidoham.socialsphere.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate

/* -------------------- Bottom Items -------------------- */

enum class BottomItem {
    HOME, CHAT, CREATE, PEOPLE, STREAM
}

/* -------------------- Main Screen -------------------- */

@Composable
fun MarketsScreen() {

    var selectedItem by remember { mutableStateOf(BottomItem.HOME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = selectedItem.name)
        }

        CustomBottomBar(
            onItemSelected = { selectedItem = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/* -------------------- Custom Bottom Bar -------------------- */

@Composable
fun CustomBottomBar(
    onItemSelected: (BottomItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {

        /* -------- Curved Green Line -------- */
        CurvedTopLine(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomIcon(Icons.Default.Home) {
                onItemSelected(BottomItem.HOME)
            }

            BottomIcon(Icons.Default.Chat) {
                onItemSelected(BottomItem.CHAT)
            }

            /* -------- Raised Create -------- */
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-20).dp)
                    .background(Color.Black, CircleShape)
                    .clickable { onItemSelected(BottomItem.CREATE) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            BottomIcon(Icons.Default.People) {
                onItemSelected(BottomItem.PEOPLE)
            }

            BottomIcon(Icons.Default.PlayArrow) {
                onItemSelected(BottomItem.STREAM)
            }
        }
    }
}

/* -------------------- Curved Line Canvas -------------------- */

@Composable
fun CurvedTopLine(modifier: Modifier = Modifier) {

    androidx.compose.foundation.Canvas(modifier = modifier) {

        val width = size.width
        val centerX = width / 2
        val radius = 80f
        val strokeWidth = 6f

        val path = Path().apply {

            /* Left straight */
            moveTo(0f, 0f)
            lineTo(centerX - radius, 0f)

            /* Left curve down */
            cubicTo(
                centerX - radius / 2, 0f,
                centerX - radius / 2, radius,
                centerX, radius
            )

            /* Right curve up */
            cubicTo(
                centerX + radius / 2, radius,
                centerX + radius / 2, 0f,
                centerX + radius, 0f
            )

            /* Right straight */
            lineTo(width, 0f)
        }

        drawPath(
            path = path,
            color = Color.Green,
            style = Stroke(width = strokeWidth)
        )
    }
}

/* -------------------- Bottom Icon -------------------- */

@Composable
fun BottomIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.Gray,
        modifier = Modifier
            .size(24.dp)
            .clickable { onClick() }
    )
}
