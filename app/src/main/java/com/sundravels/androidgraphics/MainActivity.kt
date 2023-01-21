package com.sundravels.androidgraphics

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sundravels.androidgraphics.ui.theme.AndroidGraphicsTheme
import com.sundravels.androidgraphics.ui.theme.Purple200
import com.sundravels.androidgraphics.ui.theme.Purple500
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidGraphicsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    color = MaterialTheme.colors.background
                ) {
                    PlaybackBar()
                }
            }
        }
    }
}

@Composable
fun PlaybackBar() {


    val width = with(LocalDensity.current) {
        (LocalConfiguration.current.screenWidthDp.dp - 80.dp).roundToPx()
    }

    val durationMillis = 20000


    //circle progress
    val targetProgressAnimation = remember {
        TargetBasedAnimation(
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = tween(durationMillis, easing = LinearEasing)

        )
    }
    //for bezier curves
    val targetXPositionAnimation = remember {
        TargetBasedAnimation(
            typeConverter = Float.VectorConverter, initialValue = 0f,
            targetValue = -width.toFloat(),
            animationSpec = tween(durationMillis, easing = LinearEasing)

        )
    }

    var playTime = remember {
        0L
    }

    val animationScope = rememberCoroutineScope()

    var animationState by remember {
        mutableStateOf(MediaPlaybackBarState.PAUSED)
    }

    var animationProgressValue by remember {
        mutableStateOf(0f)
    }

    var animationXPositionValue by remember {
        mutableStateOf(0f)
    }



    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
    ) {

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .height(200.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(colors = listOf(Purple500, Purple200))
                )
                .clickable {
                    //tap to pause/resume animation.
                    animationState = when (animationState) {
                        MediaPlaybackBarState.PAUSED -> MediaPlaybackBarState.RESUMED
                        MediaPlaybackBarState.RESUMED -> MediaPlaybackBarState.PAUSED
                    }
                    animationScope.launch {

                        val startTime = withFrameNanos { it } - playTime

                        while (animationState == MediaPlaybackBarState.RESUMED) {
                            playTime = withFrameNanos { it } - startTime
                            animationProgressValue =
                                targetProgressAnimation.getValueFromNanos(playTime)

                            animationXPositionValue =
                                targetXPositionAnimation.getValueFromNanos(playTime)
                        }

                    }
                }
        ) {
            Column {

                Canvas(
                    modifier = Modifier
                        .width(width.dp)
                        .height(200.dp)
                ) {

                    val position = FloatArray(2)
                    val tangent = FloatArray(2)
                    drawContext.canvas.nativeCanvas.apply {

                        val path = Path()
                        val yPath = size.height / 2
                        val xCurve = width.toFloat() / 30f
                        val yCurve = 15f

                        path.moveTo(
                            animationXPositionValue, yPath
                        )

                        for (i in 0 until width step xCurve.toInt()) {

                            when (animationState) {
                                MediaPlaybackBarState.PAUSED -> {
                                    path.rQuadTo(0f, 0f, xCurve, 0f)
                                    path.rQuadTo(0f, 0f, xCurve, 0f)
                                }
                                MediaPlaybackBarState.RESUMED -> {
                                    path.rQuadTo((xCurve / 2f), -yCurve, xCurve, 0f)
                                    path.rQuadTo((xCurve / 2f), yCurve, xCurve, 0f)
                                }

                            }
                        }

                        val measure = PathMeasure(path, false)
                        val length = measure.length

                        val partialPath = Path()
                        measure.getSegment(
                            0.0f,
                            length * (animationProgressValue / 100),
                            partialPath, true
                        )

                        partialPath.rLineTo(0f, 0.0f)
                        this.drawPath(partialPath, Paint().apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 10f
                            color = android.graphics.Color.parseColor("#FFFFFF")
                        })
                        measure.getPosTan(
                            length * (animationProgressValue / 100),
                            position,
                            tangent
                        )
                        this.drawCircle(position[0], position[1], 25f, Paint().apply {
                            color = android.graphics.Color.parseColor("#FFFFFF")
                        })
                        //draw progress line as wave increments
                        this.drawLine(
                            position[0],
                            size.height / 2,
                            length,
                            size.height / 2,
                            Paint().apply {
                                color = android.graphics.Color.parseColor("#FFFFFF")
                                strokeWidth = 10f
                            })

                    }

                }


            }

        }
    }

}

enum class MediaPlaybackBarState { PAUSED, RESUMED }

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AndroidGraphicsTheme {
        PlaybackBar()
    }
}