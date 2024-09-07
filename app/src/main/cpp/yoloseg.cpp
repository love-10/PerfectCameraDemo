#include "yoloseg.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "cpu.h"

#define MAX_STRIDE 32

static void slice(const ncnn::Mat &in, ncnn::Mat &out, int start, int end, int axis) {
    ncnn::Option opt;
    opt.num_threads = 4;
    opt.use_fp16_storage = false;
    opt.use_packing_layout = false;

    ncnn::Layer *op = ncnn::create_layer("Crop");

    // set param
    ncnn::ParamDict pd;

    ncnn::Mat axes = ncnn::Mat(1);
    axes.fill(axis);
    ncnn::Mat ends = ncnn::Mat(1);
    ends.fill(end);
    ncnn::Mat starts = ncnn::Mat(1);
    starts.fill(start);
    pd.set(9, starts);// start
    pd.set(10, ends);// end
    pd.set(11, axes);//axes

    op->load_param(pd);

    op->create_pipeline(opt);

    // forward
    op->forward(in, out, opt);

    op->destroy_pipeline(opt);

    delete op;
}

static void interp(const ncnn::Mat &in, const float &scale, const int &out_w, const int &out_h,
                   ncnn::Mat &out) {
    ncnn::Option opt;
    opt.num_threads = 4;
    opt.use_fp16_storage = false;
    opt.use_packing_layout = false;

    ncnn::Layer *op = ncnn::create_layer("Interp");

    // set param
    ncnn::ParamDict pd;
    pd.set(0, 2);// resize_type
    pd.set(1, scale);// height_scale
    pd.set(2, scale);// width_scale
    pd.set(3, out_h);// height
    pd.set(4, out_w);// width

    op->load_param(pd);

    op->create_pipeline(opt);

    // forward
    op->forward(in, out, opt);

    op->destroy_pipeline(opt);

    delete op;
}

static void reshape(const ncnn::Mat &in, ncnn::Mat &out, int c, int h, int w, int d) {
    ncnn::Option opt;
    opt.num_threads = 4;
    opt.use_fp16_storage = false;
    opt.use_packing_layout = false;

    ncnn::Layer *op = ncnn::create_layer("Reshape");

    // set param
    ncnn::ParamDict pd;

    pd.set(0, w);// start
    pd.set(1, h);// end
    if (d > 0)
        pd.set(11, d);//axes
    pd.set(2, c);//axes
    op->load_param(pd);

    op->create_pipeline(opt);

    // forward
    op->forward(in, out, opt);

    op->destroy_pipeline(opt);

    delete op;
}

static void sigmoid(ncnn::Mat &bottom) {
    ncnn::Option opt;
    opt.num_threads = 4;
    opt.use_fp16_storage = false;
    opt.use_packing_layout = false;

    ncnn::Layer *op = ncnn::create_layer("Sigmoid");

    op->create_pipeline(opt);

    // forward

    op->forward_inplace(bottom, opt);
    op->destroy_pipeline(opt);

    delete op;
}

static float fast_exp(float x) {
    union {
        uint32_t i;
        float f;
    } v{};
    v.i = (1 << 23) * (1.4426950409 * x + 126.93490512f);
    return v.f;
}

static float sigmoid(float x) {
    return 1.0f / (1.0f + fast_exp(-x));
}

static void matmul(const std::vector<ncnn::Mat> &bottom_blobs, ncnn::Mat &top_blob) {
    ncnn::Option opt;
    opt.num_threads = 2;
    opt.use_fp16_storage = false;
    opt.use_packing_layout = false;

    ncnn::Layer *op = ncnn::create_layer("MatMul");

    // set param
    ncnn::ParamDict pd;
    pd.set(0, 0);// axis

    op->load_param(pd);

    op->create_pipeline(opt);
    std::vector<ncnn::Mat> top_blobs(1);
    op->forward(bottom_blobs, top_blobs, opt);
    top_blob = top_blobs[0];

    op->destroy_pipeline(opt);

    delete op;
}

static float intersection_area(const Object &a, const Object &b) {
    cv::Rect_<float> inter = a.rect & b.rect;
    return inter.area();
}

static void qsort_descent_inplace(std::vector<Object> &faceobjects, int left, int right) {
    int i = left;
    int j = right;
    float p = faceobjects[(left + right) / 2].prob;

    while (i <= j) {
        while (faceobjects[i].prob > p)
            i++;

        while (faceobjects[j].prob < p)
            j--;

        if (i <= j) {
            // swap
            std::swap(faceobjects[i], faceobjects[j]);

            i++;
            j--;
        }
    }

    //     #pragma omp parallel sections
    {
        //         #pragma omp section
        {
            if (left < j) qsort_descent_inplace(faceobjects, left, j);
        }
        //         #pragma omp section
        {
            if (i < right) qsort_descent_inplace(faceobjects, i, right);
        }
    }
}

static void qsort_descent_inplace(std::vector<Object> &faceobjects) {
    if (faceobjects.empty())
        return;

    qsort_descent_inplace(faceobjects, 0, faceobjects.size() - 1);
}

static void nms_sorted_bboxes(const std::vector<Object> &faceobjects, std::vector<int> &picked,
                              float nms_threshold) {
    picked.clear();

    const int n = faceobjects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) {
        areas[i] = faceobjects[i].rect.width * faceobjects[i].rect.height;
    }

    for (int i = 0; i < n; i++) {
        const Object &a = faceobjects[i];

        int keep = 1;
        for (int j = 0; j < (int) picked.size(); j++) {
            const Object &b = faceobjects[picked[j]];

            // intersection over union
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            // float IoU = inter_area / union_area
            if (inter_area / union_area > nms_threshold)
                keep = 0;
        }

        if (keep)
            picked.push_back(i);
    }
}

static void generate_proposals(std::vector<GridAndStride> grid_strides, const ncnn::Mat &pred,
                               float prob_threshold, std::vector<Object> &objects) {
    const int num_points = grid_strides.size();
    const int num_class = 80;
    const int reg_max_1 = 16;

    for (int i = 0; i < num_points; i++) {
        const float *scores = pred.row(i) + 4 * reg_max_1;

        // find label with max score
        int label = -1;
        float score = -FLT_MAX;
        for (int k = 0; k < num_class; k++) {
            float confidence = scores[k];
            if (confidence > score) {
                label = k;
                score = confidence;
            }
        }
        float box_prob = sigmoid(score);
        if (box_prob >= prob_threshold) {
            ncnn::Mat bbox_pred(reg_max_1, 4, (void *) pred.row(i));
            {
                ncnn::Layer *softmax = ncnn::create_layer("Softmax");

                ncnn::ParamDict pd;
                pd.set(0, 1); // axis
                pd.set(1, 1);
                softmax->load_param(pd);

                ncnn::Option opt;
                opt.num_threads = 1;
                opt.use_packing_layout = false;

                softmax->create_pipeline(opt);

                softmax->forward_inplace(bbox_pred, opt);

                softmax->destroy_pipeline(opt);

                delete softmax;
            }

            float pred_ltrb[4];
            for (int k = 0; k < 4; k++) {
                float dis = 0.f;
                const float *dis_after_sm = bbox_pred.row(k);
                for (int l = 0; l < reg_max_1; l++) {
                    dis += l * dis_after_sm[l];
                }

                pred_ltrb[k] = dis * grid_strides[i].stride;
            }

            float pb_cx = (grid_strides[i].grid0 + 0.5f) * grid_strides[i].stride;
            float pb_cy = (grid_strides[i].grid1 + 0.5f) * grid_strides[i].stride;

            float x0 = pb_cx - pred_ltrb[0];
            float y0 = pb_cy - pred_ltrb[1];
            float x1 = pb_cx + pred_ltrb[2];
            float y1 = pb_cy + pred_ltrb[3];

            Object obj;
            obj.rect.x = x0;
            obj.rect.y = y0;
            obj.rect.width = x1 - x0;
            obj.rect.height = y1 - y0;
            obj.label = label;
            obj.prob = box_prob;
            obj.mask_feat.resize(32);
            std::copy(pred.row(i) + 64 + num_class, pred.row(i) + 64 + num_class + 32,
                      obj.mask_feat.begin());
            objects.push_back(obj);
        }
    }
}

static void
generate_grids_and_stride(const int target_w, const int target_h, std::vector<int> &strides,
                          std::vector<GridAndStride> &grid_strides) {
    for (int i = 0; i < (int) strides.size(); i++) {
        int stride = strides[i];
        int num_grid_w = target_w / stride;
        int num_grid_h = target_h / stride;
        for (int g1 = 0; g1 < num_grid_h; g1++) {
            for (int g0 = 0; g0 < num_grid_w; g0++) {
                GridAndStride gs;
                gs.grid0 = g0;
                gs.grid1 = g1;
                gs.stride = stride;
                grid_strides.push_back(gs);
            }
        }
    }
}

static void decode_mask(const ncnn::Mat &mask_feat, const int &img_w, const int &img_h,
                        const ncnn::Mat &mask_proto, const ncnn::Mat &in_pad, const int &wpad,
                        const int &hpad,
                        ncnn::Mat &mask_pred_result) {
    ncnn::Mat masks;
    matmul(std::vector<ncnn::Mat>{mask_feat, mask_proto}, masks);
    sigmoid(masks);
    reshape(masks, masks, masks.h, in_pad.h / 4, in_pad.w / 4, 0);
    slice(masks, mask_pred_result, (wpad / 2) / 4, (in_pad.w - wpad / 2) / 4, 2);
    slice(mask_pred_result, mask_pred_result, (hpad / 2) / 4, (in_pad.h - hpad / 2) / 4, 1);
    interp(mask_pred_result, 4.0, img_w, img_h, mask_pred_result);

}


YoloSeg::YoloSeg() {
    blob_pool_allocator.set_size_compare_ratio(0.f);
    workspace_pool_allocator.set_size_compare_ratio(0.f);
}

int YoloSeg::load(AAssetManager *mgr) {
    yolo.clear();
    blob_pool_allocator.clear();
    workspace_pool_allocator.clear();

    ncnn::set_cpu_powersave(2);
    ncnn::set_omp_num_threads(ncnn::get_big_cpu_count());

    yolo.opt = ncnn::Option();

    yolo.opt.num_threads = ncnn::get_big_cpu_count();
    yolo.opt.blob_allocator = &blob_pool_allocator;
    yolo.opt.workspace_allocator = &workspace_pool_allocator;

    char parampath[256] = "yolov8n-seg-sim-opt-fp16.param";
    char modelpath[256] = "yolov8n-seg-sim-opt-fp16.bin";

    yolo.load_param(mgr, parampath);
    yolo.load_model(mgr, modelpath);

    target_size = 320;
    norm_vals[0] = 1 / 255.f;
    norm_vals[1] = 1 / 255.f;
    norm_vals[2] = 1 / 255.f;

    return 0;
}

int YoloSeg::detect(const cv::Mat &rgb, std::vector<Object> &objects, bool needMark,
                    float prob_threshold,
                    float nms_threshold) {
    int width = rgb.cols;
    int height = rgb.rows;

    // pad to multiple of 32
    int w = width;
    int h = height;
    float scale = 1.f;
    if (w > h) {
        scale = (float) target_size / w;
        w = target_size;
        h = h * scale;
    } else {
        scale = (float) target_size / h;
        h = target_size;
        w = w * scale;
    }

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb.data, ncnn::Mat::PIXEL_RGB2BGR, width, height,
                                                 w, h);

    // pad to target_size rectangle
    int wpad = (w + MAX_STRIDE - 1) / MAX_STRIDE * MAX_STRIDE - w;
    int hpad = (h + MAX_STRIDE - 1) / MAX_STRIDE * MAX_STRIDE - h;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2,
                           ncnn::BORDER_CONSTANT, 0.f);

    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = yolo.create_extractor();

    ex.input("images", in_pad);

    ncnn::Mat out;
    ex.extract("output0", out);

    ncnn::Mat mask_proto;
    if (needMark) {
        ex.extract("output1", mask_proto);
    }


    std::vector<int> strides = {8, 16, 32}; // might have stride=64
    std::vector<GridAndStride> grid_strides;
    generate_grids_and_stride(in_pad.w, in_pad.h, strides, grid_strides);

    std::vector<Object> proposals;
    std::vector<Object> objects8;
    generate_proposals(grid_strides, out, prob_threshold, objects8);
    proposals.insert(proposals.end(), objects8.begin(), objects8.end());

    // sort all proposals by score from highest to lowest
    qsort_descent_inplace(proposals);

    // apply nms with nms_threshold
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, nms_threshold);

    int count = picked.size();

    ncnn::Mat mask_feat = ncnn::Mat(32, count, sizeof(float));
    for (int i = 0; i < count; i++) {
        float *mask_feat_ptr = mask_feat.row(i);
        std::memcpy(mask_feat_ptr, proposals[picked[i]].mask_feat.data(),
                    sizeof(float) * proposals[picked[i]].mask_feat.size());
    }

    ncnn::Mat mask_pred_result;
    if (needMark) {
        decode_mask(mask_feat, width, height, mask_proto, in_pad, wpad, hpad, mask_pred_result);
    }

    objects.resize(count);
    for (int i = 0; i < count; i++) {
        objects[i] = proposals[picked[i]];

        // adjust offset to original unpadded
        float x0 = (objects[i].rect.x - (wpad / 2)) / scale;
        float y0 = (objects[i].rect.y - (hpad / 2)) / scale;
        float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale;
        float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale;

        // clip
        x0 = std::max(std::min(x0, (float) (width - 1)), 0.f);
        y0 = std::max(std::min(y0, (float) (height - 1)), 0.f);
        x1 = std::max(std::min(x1, (float) (width - 1)), 0.f);
        y1 = std::max(std::min(y1, (float) (height - 1)), 0.f);

        objects[i].rect.x = x0;
        objects[i].rect.y = y0;
        objects[i].rect.width = x1 - x0;
        objects[i].rect.height = y1 - y0;
        if (needMark) {
            objects[i].mask = cv::Mat::zeros(height, width, CV_32FC1);
            cv::Mat mask = cv::Mat(height, width, CV_32FC1, (float *) mask_pred_result.channel(i));
            mask(objects[i].rect).copyTo(objects[i].mask(objects[i].rect));
        }
    }

    return 0;
}

cv::Rect getCropRect(cv::Mat bitmap, float scale) {
    int width = bitmap.cols;
    int height = bitmap.rows;
    int left = (width - width * scale) / 2;
    int top = (height - height * scale) / 2;
    cv::Rect ret(left, top, width * scale, height * scale);
    return ret;
}

// 添加光斑滤镜函数
void addBokehEffect(cv::Mat &image, int numCircles, float minRadius, float maxRadius) {
    // 随机数种子
    std::srand(std::time(0));

    // 循环生成随机光斑
    for (int i = 0; i < numCircles; ++i) {
        // 随机选择圆心位置
        int x = std::rand() % image.cols;
        int y = std::rand() % image.rows;

        // 随机生成半径
        float radius =
                minRadius + static_cast<float>(std::rand()) / RAND_MAX * (maxRadius - minRadius);

        // 随机选择颜色
        cv::Scalar color(std::rand() % 256, std::rand() % 256, std::rand() % 256);

        // 绘制圆形光斑
        cv::circle(image, cv::Point(x, y), static_cast<int>(radius), color, -1, cv::INTER_LINEAR);
    }
}

// 还原人像位置的像素
void resetPerson(cv::Mat origin, cv::Mat &effect, cv::Mat mask) {
    int w = origin.cols;
    int h = origin.rows;
    for (int i = 0; i < h; i++) {
        const float *mask_ptr = mask.ptr<float>(i);
        for (int j = 0; j < w; j++) {
            if (mask_ptr[j] >= 0.5) {
                effect.at<cv::Vec3b>(i, j) = origin.at<cv::Vec3b>(i, j);
            }
        }
    }
}

bool hasPerson(const std::vector<Object> &objects) {
    cv::Mat ret;
    for (int i = 0; i < objects.size(); i++) {
        const Object &obj = objects[i];
        if (obj.label == 0) {
            return true;
        }
    }
    return false;
}

cv::Mat getPersonMask(const std::vector<Object> &objects) {
    cv::Mat ret;
    for (int i = 0; i < objects.size(); i++) {
        const Object &obj = objects[i];
        if (obj.label == 0) {
            ret = obj.mask;
            break;
        }
    }
    return ret;
}

// 将图像转换为复古 (Sepia) 效果
void applyVintageEffect(const cv::Mat& src) {
    // 应用 Sepia 滤镜
    cv::Mat sepiaKernel = (cv::Mat_<float>(3, 3) <<
                                                 0.393, 0.769, 0.189,
            0.349, 0.686, 0.168,
            0.272, 0.534, 0.131);

    cv::transform(src, src, sepiaKernel);

    // 将值截断到有效范围
    cv::normalize(src, src, 0, 255, cv::NORM_MINMAX);
    src.convertTo(src, CV_8UC3);
}

// 添加噪声来模拟旧照片的效果
void addNoise(cv::Mat& image) {
    cv::randn(image, 0, 30);  // 正态分布噪声，标准差为30
}

int YoloSeg::draw(cv::Mat &rgb, const std::vector<Object> &objects, bool needMark) {
    if (objects.size() == 0 || !hasPerson(objects) || !needMark) {
        return 0;
    }
    cv::Mat personMask = getPersonMask(objects);

    cv::Rect cropArea = getCropRect(rgb, 1);

    // 提取 ROI
    cv::Mat effect = rgb(cropArea);
    cv::Mat mask = personMask(cropArea);
    cv::Mat origin = effect.clone();

//    applyVintageEffect(effect);
//    addNoise(effect);

    addBokehEffect(effect, 50, 2.0f, 15.0f);  // 100个光斑，最小半径为10，最大为50
    cv::GaussianBlur(effect, effect, cv::Size(15, 15), 0);

    resetPerson(origin, effect, mask);

    return 0;
}
