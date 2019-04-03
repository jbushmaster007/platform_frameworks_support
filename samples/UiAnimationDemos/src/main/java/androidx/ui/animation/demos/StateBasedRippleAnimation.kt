/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.animation.demos

import android.app.Activity
import android.graphics.PointF
import android.os.Bundle
import androidx.animation.FloatPropKey
import androidx.animation.InterruptionHandling
import androidx.animation.TransitionDefinition
import androidx.animation.TransitionState
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.MeasureBox
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.ipx
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Composable
import com.google.r4a.Recompose
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.setContent
import com.google.r4a.unaryPlus

class StateBasedRippleAnimation : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { <StateBasedRippleDemo /> }
    }
}

@Composable
fun StateBasedRippleDemo() {
    <CraneWrapper>
        <MeasureBox> constraints ->
            val placeable = collect {
                <RippleRect width=constraints.maxWidth height=constraints.maxHeight />
            }.firstOrNull()?.measure(constraints)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable?.place(0.ipx, 0.ipx)
            }
        </MeasureBox>
    </CraneWrapper>
}

@Composable
fun RippleRect(width: IntPx, height: IntPx) {
    if (width.value == 0 || height.value == 0) {
        return
    }
    val targetRadius: Float = 30f + Math.sqrt(
        (
                width.value * width.value + height.value * height.value).toDouble()
    ).toFloat() / 2f
    var toState = ButtonStatus.Released
    val rippleTransDef = +memo(targetRadius) { createTransDef(targetRadius) }
    <Recompose> recompose ->
        val onPress: (PxPosition) -> Unit = { position ->
            toState = ButtonStatus.Pressed
            down.x = position.x.value
            down.y = position.y.value
            recompose()
        }

        val onRelease: () -> Unit = {
            toState = ButtonStatus.Released
            recompose()
        }
        <PressGestureDetector onPress onRelease>
            <Layout layoutBlock = { _, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {}
            }>
                <Transition definition=rippleTransDef toState> state ->
                    <RippleRectFromState state />
                </Transition>
            </Layout>
        </PressGestureDetector>
    </Recompose>
}

@Composable
fun RippleRectFromState(state: TransitionState) {

    // TODO: file bug for when "down" is not a file level val, it's not memoized correctly
    val x = down.x
    val y = down.y

    val paint =
        Paint().apply { color = Color.fromARGB(
            (state[androidx.ui.animation.demos.alpha] * 255).toInt(), 0, 235, 224) }

    val radius = state[radius]

    <Draw> canvas, pixelSize ->
        canvas.drawCircle(Offset(x, y), radius, paint)
    </Draw>
}

private val down = PointF(0f, 0f)

private val alpha = FloatPropKey()
private val radius = FloatPropKey()

private fun createTransDef(targetRadius: Float): TransitionDefinition<ButtonStatus> {
    return transitionDefinition {
        state(ButtonStatus.Released) {
            this[alpha] = 0f
            this[radius] = targetRadius * 0.3f
        }
        state(ButtonStatus.Pressed) {
            this[alpha] = 0.2f
            this[radius] = targetRadius + 15f
        }

        // Grow the ripple
        transition(fromState = ButtonStatus.Released, toState = ButtonStatus.Pressed) {
            alpha using keyframes {
                duration = 225
                0f at 0
                0.2f at 75
                0.2f at 225
            }

            radius using tween {
                duration = 225
            }
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }

        // Fade out the ripple
        transition(fromState = ButtonStatus.Pressed, toState = ButtonStatus.Released) {
            alpha using tween {
                duration = 150
            }

            radius using keyframes {
                duration = 150
                targetRadius + 15f at 0 // optional
                targetRadius + 15f at (duration - 1)
                targetRadius * 0.3f at duration // optional
            }
        }
    }
}
