// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolo.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON

#include <arm_neon.h>

#endif // __ARM_NEON

JavaVM *g_vm = NULL;
jclass bClz;
jobject bObj;

static int draw_unsupported(cv::Mat &rgb) {
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y),
                                cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat &rgb) {
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f) {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--) {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f) {
            return 0;
        }

        for (int i = 0; i < 10; i++) {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y),
                                cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

jobject mattobitmap(JNIEnv *env, cv::Mat mat) {
// 从传递的地址获取Mat对象


// 创建一个空的Bitmap对象
    jobject bitmap;
    AndroidBitmapInfo info;
    void *pixels;

    int width = mat.cols;
    int height = mat.rows;

// 创建Bitmap对象
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID valueOfMethod = env->GetStaticMethodID(bitmapConfigClass, "valueOf",
                                                     "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, valueOfMethod,
                                                       configName);
    bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, width, height,
                                         bitmapConfig);

// 将Mat对象转换为Bitmap
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return nullptr;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return nullptr;
    }
    cv::Mat temp(info.height, info.width, CV_8UC4, pixels);
    if (mat.type() == CV_8UC1) {
        cvtColor(mat, temp, cv::COLOR_GRAY2RGBA);
    } else if (mat.type() == CV_8UC3) {
        cvtColor(mat, temp, cv::COLOR_BGR2RGBA);
    } else if (mat.type() == CV_8UC4) {
        mat.copyTo(temp);
    }
    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

static Yolo *g_yolo = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow {
public:
    virtual void on_image_render(cv::Mat &rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat &rgb) const {
    JNIEnv *env;
    if (g_vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        // 当前线程不附加到JavaVM，需要附加它
        if (g_vm->AttachCurrentThread(reinterpret_cast<JNIEnv **>((void **) &env), NULL) != 0) {
            // 无法附加线程，处理错误
        }
    }
    // 使用全局引用进行操作
    if (bObj != NULL) {
        // 执行需要的JNI操作，例如调用Java方法
//        jclass clazz = (*env).GetObjectClass(bObj);
        jmethodID methodID = (*env).GetMethodID(bClz, "onBitmap", "(Landroid/graphics/Bitmap;)V");
        jobject bitmap = mattobitmap(env, rgb);
        if (methodID != NULL) {
            (*env).CallVoidMethod(bObj, methodID, bitmap);
        }
    }
    // 在这里使用env指针执行需要的JNI操作

    // 完成后，如果线程不是由JavaVM附加的，需要分离它
    g_vm->DetachCurrentThread();
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo) {
            std::vector<Object> objects;

            g_yolo->detect(rgb, objects);

            g_yolo->draw(rgb, objects);
        } else {
            draw_unsupported(rgb);
        }
    }

    draw_fps(rgb);
}

static MyNdkCamera *g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");
    g_vm = vm;
    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
extern "C" jboolean
Java_com_example_perfectcamerademo_Yolo6_loadModel(JNIEnv *env, jobject thiz, jobject assetManager,
                                                   jint modelid, jint cpugpu, jobject bCallBack) {
    if (modelid < 0 || modelid > 4 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    jclass clz = (*env).GetObjectClass(bCallBack);
    bClz = static_cast<jclass>((*env).NewGlobalRef(clz));
    bObj = (*env).NewGlobalRef(bCallBack);

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char *modeltypes[] =
            {
                    "lite-s",
                    "lite-m",
                    "lite-l0",
                    "lite-l1",
                    "lite-l2",
            };

    const int target_sizes[][2] =
            {
                    {320, 320},
                    {320, 320},
                    {320, 320},
                    {320, 192},
                    {224, 128}
            };

    const float mean_vals[][3] =
            {
                    {0.f, 0.f, 0.f},
                    {0.f, 0.f, 0.f},
                    {0.f, 0.f, 0.f},
                    {0.f, 0.f, 0.f},
                    {0.f, 0.f, 0.f}
            };

    const float norm_vals[][3] =
            {
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f}
            };

    const char *modeltype = modeltypes[(int) modelid];
    const int *target_size = target_sizes[(int) modelid];
    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_yolo;
            g_yolo = 0;
        } else {
            if (!g_yolo)
                g_yolo = new Yolo;
            g_yolo->load(mgr, modeltype, target_size, mean_vals[(int) modelid],
                         norm_vals[(int) modelid], use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
extern "C" jboolean
Java_com_example_perfectcamerademo_Yolo6_openCamera(JNIEnv *env, jobject thiz, jint facing,
                                                    jint the_camera_id) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int) facing, (int) the_camera_id);

    return JNI_TRUE;
}

// public native boolean closeCamera();
extern "C" jboolean
Java_com_example_perfectcamerademo_Yolo6_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
extern "C" jboolean
Java_com_example_perfectcamerademo_Yolo6_setOutputWindow(JNIEnv *env, jobject thiz,
                                                         jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

}
