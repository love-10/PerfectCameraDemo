cmake_minimum_required(VERSION 3.22.1)

project(perfectcamerademo)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/jni)

set(OpenCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/opencv-mobile-2.4.13.7-android/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc)

set(ncnn_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ncnn-20240410-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(yolov6ncnn SHARED yolov6ncnn.cpp yolo.cpp)

target_link_libraries(yolov6ncnn ncnn ${OpenCV_LIBS} camera2ndk mediandk)
