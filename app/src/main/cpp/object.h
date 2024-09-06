//
// Created by 梦之流风 on 2024/9/7.
//
#include <opencv2/core/core.hpp>
#ifndef PERFECTCAMERADEMO_OBJECT_H
#define PERFECTCAMERADEMO_OBJECT_H
struct Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
    cv::Mat mask;
    std::vector<float> mask_feat;
};
struct GridAndStride
{
    int grid0;
    int grid1;
    int stride;
};
#endif //PERFECTCAMERADEMO_OBJECT_H
