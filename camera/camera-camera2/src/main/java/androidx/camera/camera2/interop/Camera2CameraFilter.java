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

package androidx.camera.camera2.interop;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.core.util.Preconditions;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides ability to filter cameras with camera IDs and characteristics and create the
 * corresponding {@link CameraFilter}.
 */
@ExperimentalCamera2Interop
public final class Camera2CameraFilter {

    /**
     * Creates a {@link CameraFilter} from a {@link Camera2Filter}.
     */
    @NonNull
    public static CameraFilter createCameraFilter(@NonNull Camera2Filter filter) {
        return (cameras) -> {
            LinkedHashMap<String, Camera> cameraMap = new LinkedHashMap<>();
            LinkedHashMap<String, CameraCharacteristics> characteristicsMap =
                    new LinkedHashMap<>();
            for (Camera camera : cameras) {
                CameraInfo cameraInfo = camera.getCameraInfo();
                Preconditions.checkState(cameraInfo instanceof Camera2CameraInfoImpl,
                        "CameraInfo does not contain any Camera2 information.");
                Camera2CameraInfoImpl camera2CameraInfoImpl =
                        (Camera2CameraInfoImpl) cameraInfo;
                cameraMap.put(camera2CameraInfoImpl.getCameraId(), camera);
                characteristicsMap.put(camera2CameraInfoImpl.getCameraId(),
                        camera2CameraInfoImpl.getCameraCharacteristics());
            }

            filter.filter(characteristicsMap);

            Set<Camera> resultCameras = new LinkedHashSet<>();
            for (Map.Entry<String, CameraCharacteristics> entry :
                    characteristicsMap.entrySet()) {
                String cameraId = entry.getKey();
                // The extra camera IDs not contained in the camera map will be ignored.
                if (cameraMap.containsKey(cameraId)) {
                    resultCameras.add(cameraMap.get(cameraId));
                } else {
                    throw new IllegalArgumentException(
                            "There are camera IDs not contained in the original camera map.");
                }
            }
            cameras.retainAll(resultCameras);
        };
    }

    /**
     * An interface that filters cameras based on camera IDs and characteristics. Applications
     * can implement the filter method for custom camera selection.
     */
    public interface Camera2Filter {
        /**
         * Filters the input camera IDs based on their {@link CameraCharacteristics}. The method
         * modifies the input map directly, leaves the entries that match requirement and remove
         * the rest.
         *
         * <p>If the filtered map has extra camera IDs not contained in the original map, when
         * used by a {@link androidx.camera.core.CameraSelector} then it will result in an
         * IllegalArgumentException thrown when calling bindToLifecycle.
         *
         * <p>The camera ID that has lower index in the map has higher priority. When used by
         * {@link androidx.camera.core.CameraSelector.Builder#addCameraFilter(CameraFilter)}, the
         * available cameras will be filtered by the {@link Camera2CameraFilter} and all other
         * {@link CameraFilter}s by the order they were added. The first camera in the result
         * will be selected if there are multiple cameras left.
         *
         * @param idCharacteristicsMap The map of camera ID and {@link CameraCharacteristics} of
         *                             the cameras being filtered.
         */
        void filter(@NonNull LinkedHashMap<String, CameraCharacteristics> idCharacteristicsMap);
    }

    // Should not be instantiated.
    private Camera2CameraFilter() {}
}
