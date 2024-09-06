#ifndef YOLOV8_SEG_H
#define YOLOV8_SEG_H



#include <net.h>
#include "object.h"


class YoloSeg
{
public:
    YoloSeg();

    int load(AAssetManager* mgr);

    int detect(const cv::Mat& rgb, std::vector<Object>& objects, bool needMark = false, float prob_threshold = 0.4f, float nms_threshold = 0.5f);

    int draw(cv::Mat& rgb, const std::vector<Object>& objects, bool needMark = false);

private:
    ncnn::Net yolo;

    int target_size;
    float norm_vals[3];

    ncnn::UnlockedPoolAllocator blob_pool_allocator;
    ncnn::PoolAllocator workspace_pool_allocator;
};

#endif // YOLOV8_SEG_H
