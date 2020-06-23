/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.rotations.imageanalysis

import androidx.camera.core.CameraSelector
import androidx.camera.integration.uiwidgets.rotations.CameraActivity
import androidx.camera.integration.uiwidgets.rotations.LockedOrientationActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class LockedOrientationActivityTest(private val lensFacing: Int, private val rotationDegrees: Int) :
    ImageAnalysisTest<LockedOrientationActivity>() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, rotationDegrees={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf(CameraSelector.LENS_FACING_BACK, 0))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, 90))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, 180))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, 270))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, 0))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, 90))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, 180))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, 270))
        }
    }

    @get:Rule
    val mCameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*CameraActivity.PERMISSIONS)

    @Before
    fun before() {
        setUp(lensFacing)
    }

    @After
    fun after() {
        tearDown()
    }

    @Test
    fun verifyRotation() {
        verifyRotation<LockedOrientationActivity>(lensFacing) {
            rotate(rotationDegrees)
        }
    }

    private fun ActivityScenario<LockedOrientationActivity>.rotate(rotationDegrees: Int) {
        onActivity { activity ->
            activity.mOrientationEventListener.onOrientationChanged(rotationDegrees)
        }
    }
}
