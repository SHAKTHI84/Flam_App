# Flam App - Android OpenCV Camera Filter

This is an Android application that uses the device camera to apply a real-time Canny edge detection filter using OpenCV and displays the result on the screen.

## Features Implemented

*   **Live Camera Preview**: Utilizes Android's `Camera2` API for efficient, modern camera access.
*   **Real-time Edge Detection**: Processes the live camera feed with OpenCV's Canny edge detector via a C++ JNI bridge.
*   **OpenGL Rendering**: Displays the processed video feed on a `GLSurfaceView` for high performance.
*   **Front/Back Camera Switching**: A floating action button allows toggling between the device's front and back cameras.
*   **Image Capture**: A capture button saves the currently displayed processed frame (with the filter applied) to the device's public photo gallery.
*   **Runtime Permissions**: Correctly handles camera permissions at runtime.

*Note: The web and TypeScript components mentioned in the assignment criteria were not part of this development process.*

## Screenshots

**To be added by the author.**

*Suggestion: Include a screenshot of the app running with the back camera, one with the front camera, and a screenshot of a captured image from the device's gallery.*

(Here is a placeholder for your image)
`![App Screenshot](path/to/your/screenshot.png)`

## Setup Instructions

To build and run this project, you will need to configure the following dependencies:

1.  **Clone the Repository**:
    ```sh
    git clone https://github.com/SHAKTHI84/Flam_App.git
    cd Flam_App
    ```

2.  **Android NDK**:
    *   This project uses the Native Development Kit (NDK) to compile the C++ code.
    *   Go to **Tools > SDK Manager** in Android Studio.
    *   Under the **SDK Tools** tab, check **NDK (Side by side)** and click Apply to install it.
    *   The project is configured to use a recent version of the NDK, and Android Studio will handle the rest.

3.  **OpenCV Android SDK**:
    *   Download the OpenCV Android SDK (e.g., version 4.x) from the [official OpenCV website](https://opencv.org/releases/).
    *   Unzip the downloaded file.
    *   Copy the `OpenCV-android-sdk` directory from the unzipped folder and place it in the **root directory** of this project.
    *   The project build files (`app/build.gradle.kts` and `app/src/main/jni/CMakeLists.txt`) are already configured to find the SDK in this location.

4.  **Build and Run**:
    *   Open the project in Android Studio.
    *   Let Gradle sync the project.
    *   Build and run the app on a physical Android device.

## Architecture Explained

The application's architecture is based on a clean separation between the Android UI/Camera logic, the high-performance rendering code, and the native C++ image processing code.

### Frame Flow

1.  **Camera2 API**: `MainActivity` uses the `Camera2` API to configure the camera and request a stream of frames.
2.  **ImageReader**: An `ImageReader` is set up to receive camera frames in `YUV_420_888` format. This is an efficient format that provides direct access to the raw sensor data.
3.  **JNI Bridge**: For each new frame, the `OnImageAvailableListener` extracts the grayscale (Y) plane and its memory layout details (`rowStride`). This data is passed directly to a native C++ function (`processFrame`) using the Java Native Interface (JNI).

### JNI and OpenCV

*   The C++ code resides in `app/src/main/jni/native-lib.cpp`.
*   The `processFrame` function receives the grayscale image data from Kotlin.
*   It wraps this data in an `cv::Mat` object, being careful to use the `rowStride` to account for memory padding.
*   It then applies the **Canny edge detection** algorithm using `cv::Canny`.
*   The resulting single-channel (black and white) edge map is converted into a 4-channel **RGBA** image, which is required for rendering with OpenGL.
*   This final RGBA data is returned to the Kotlin layer as a `ByteArray`.

### OpenGL ES Rendering

*   `MyGLRenderer.kt` manages all OpenGL rendering on a dedicated `GLSurfaceView`.
*   When the processed RGBA data arrives from the JNI layer, it is passed to the renderer.
*   The renderer safely uploads the byte array to the GPU as a texture on the GL rendering thread to avoid threading conflicts.
*   This texture is then drawn onto a simple 2D rectangle (a quad) that fills the view, displaying the final, processed image to the user.
*   The renderer also handles rotation and mirroring by applying different texture coordinates when the front camera is active.
