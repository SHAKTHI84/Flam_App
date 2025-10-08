#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

using namespace cv;

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_myapplication_MainActivity_processFrame(
        JNIEnv* env,
        jobject /* this */,
        jint width,
        jint height,
        jbyteArray gray_bytes,
        jint row_stride) {

    jbyte* gray_data = env->GetByteArrayElements(gray_bytes, nullptr);

    // Create a Mat from the input grayscale data
    Mat gray_mat_with_stride(height, width, CV_8UC1, gray_data, row_stride);
    Mat edges_mat;
    Mat rgba_mat;

    // Apply Canny edge detection
    Canny(gray_mat_with_stride, edges_mat, 50, 150);

    // Convert the single-channel edge map to a 4-channel RGBA image for rendering
    cvtColor(edges_mat, rgba_mat, COLOR_GRAY2RGBA);

    jsize total_bytes = rgba_mat.total() * rgba_mat.elemSize();
    jbyteArray result_byte_array = env->NewByteArray(total_bytes);

    if (result_byte_array == nullptr) {
        env->ReleaseByteArrayElements(gray_bytes, gray_data, JNI_ABORT);
        return nullptr; // Out of memory
    }

    env->SetByteArrayRegion(result_byte_array, 0, total_bytes, reinterpret_cast<jbyte*>(rgba_mat.data));

    env->ReleaseByteArrayElements(gray_bytes, gray_data, JNI_ABORT);

    return result_byte_array;
}
