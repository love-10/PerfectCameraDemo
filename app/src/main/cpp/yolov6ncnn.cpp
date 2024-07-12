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

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON

#include <arm_neon.h>

#endif // __ARM_NEON

static Yolo *g_yolo = 0;
static ncnn::Mutex lock;

#define ASSERT(status, ret)     if (!(status)) { return ret; }
#define ASSERT_FALSE(status)    ASSERT(status, false)


bool BitmapToMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    void *bitmapPixels;                                            // Save picture pixel data
    AndroidBitmapInfo bitmapInfo;                                   // Save picture parameters

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >=
                 0)        // Get picture parameters
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format ==
                    ANDROID_BITMAP_FORMAT_RGB_565)         // Only ARGB? 8888 and RGB? 565 are supported
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >=
                 0) // Get picture pixels (lock memory block)
    ASSERT_FALSE(bitmapPixels)

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4,
                    bitmapPixels);    // Establish temporary mat
        tmp.copyTo(
                matrix);                                                         // Copy to target matrix
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_BGR5652RGB);
    }

    //convert RGB to BGR
    cv::cvtColor(matrix, matrix, cv::COLOR_RGB2BGR);

    AndroidBitmap_unlockPixels(env, obj_bitmap);            // Unlock
    return true;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_tracker_Yolov6Ncnn_detect(JNIEnv *env, jobject thiz, jobject bitmap) {
    cv::Mat mat;
    BitmapToMatrix(env, bitmap, mat);
    std::vector<Object> objects;
    g_yolo->detect(mat, objects);

    jclass list_cls = env->FindClass("java/util/ArrayList");//获得ArrayList类引用

    jmethodID list_costruct = env->GetMethodID(list_cls, "<init>", "()V"); //获得得构造函数Id

    jobject list_obj = env->NewObject(list_cls, list_costruct); //创建一个Arraylist集合对象
    //或得Arraylist类中的 add()方法ID，其方法原型为： boolean add(Object object) ;
    jmethodID list_add = env->GetMethodID(list_cls, "add", "(Ljava/lang/Object;)Z");

    jclass stu_cls = env->FindClass("com/example/tracker/Box");//获得Student类引用
    //获得该类型的构造函数  函数名为 <init> 返回类型必须为 void 即 V
    jmethodID stu_costruct = env->GetMethodID(stu_cls, "<init>", "(FFFFFI)V");

    for (int i = 0; i < objects.size(); i++) {
        const Object &obj = objects[i];
        //通过调用该对象的构造函数来new 一个 Student实例
        jobject stu_obj = env->NewObject(stu_cls, stu_costruct, obj.rect.x, obj.rect.y,
                                         obj.rect.width, obj.rect.height, obj.prob,
                                         obj.label);  //构造一个对象

        env->CallBooleanMethod(list_obj, list_add, stu_obj); //执行Arraylist类实例的add方法，添加一个stu对象
    }

    return list_obj;
}


extern "C" {

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL
Java_com_example_tracker_Yolov6Ncnn_loadModel(JNIEnv *env, jobject thiz, jobject assetManager,
                                                 jint modelid, jint cpugpu) {
    if (modelid < 0 || modelid > 4 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

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
}
