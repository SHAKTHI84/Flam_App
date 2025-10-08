cmake_minimum_required(VERSION 3.22.1)

project("myapplication")

# Path to OpenCV headers
include_directories("C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/jni/include")

add_library(myapplication SHARED native-lib.cpp)

# Find Android logging and zlib libraries
find_library(log-lib log)
find_library(z-lib z)

# --- Architecture Specific Library Linking ---

# Common libraries for all architectures
set(OPENCV_3RDPARTY_LIBS_COMMON
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libade.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libcpufeatures.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libIlmImf.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libittnotify.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/liblibjpeg-turbo.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/liblibopenjp2.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/liblibpng.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/liblibprotobuf.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/liblibtiff.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/liblibwebp.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libtbb.a"
)

if(ANDROID_ABI STREQUAL "armeabi-v7a" OR ANDROID_ABI STREQUAL "arm64-v8a")
    set(OPENCV_3RDPARTY_LIBS ${OPENCV_3RDPARTY_LIBS_COMMON}
        "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libtegra_hal.a"
    )
elseif(ANDROID_ABI STREQUAL "x86" OR ANDROID_ABI STREQUAL "x86_64")
    set(OPENCV_3RDPARTY_LIBS ${OPENCV_3RDPARTY_LIBS_COMMON}
        "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libippiw.a"
        "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/3rdparty/libs/${ANDROID_ABI}/libippicv.a"
    )
endif()

# Main OpenCV Modules
set(OPENCV_MAIN_LIBS
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/staticlibs/${ANDROID_ABI}/libopencv_core.a"
    "C:/Users/Rohith/AndroidStudioProjects/MyApplication/OpenCV-android-sdk/native/staticlibs/${ANDROID_ABI}/libopencv_imgproc.a"
)

# Link all necessary libraries
target_link_libraries(myapplication
    ${log-lib}
    ${z-lib} # Link zlib
    ${OPENCV_MAIN_LIBS}
    ${OPENCV_3RDPARTY_LIBS}
)
