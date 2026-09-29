package com.example.mvt.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mvt.R
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun MvtLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = PrimaryBlue,
                strokeWidth = 6.dp,
                modifier = Modifier.width(110.dp).height(110.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.mvt),
                contentDescription = "MVT",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(63.dp).height(27.dp)
            )
        }
    }
}
