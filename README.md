import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfPoint

fun main() {
    // 初始化OpenCV
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

    // 读取图像
    val src = Imgcodecs.imread("path_to_image.jpg")

    // 转换为灰度图像
    val gray = Mat()
    Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

    // 使用高斯模糊平滑图像
    Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

    // 使用Canny边缘检测
    val edges = Mat()
    Imgproc.Canny(gray, edges, 50.0, 150.0)

    // 查找轮廓
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

    // 遍历所有轮廓，筛选出矩形轮廓
    for (contour in contours) {
        val approxCurve = MatOfPoint2f()
        val contour2f = MatOfPoint2f(*contour.toArray())
        val approxDistance = Imgproc.arcLength(contour2f, true) * 0.02
        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true)
        val points = approxCurve.toArray()

        // 筛选出矩形（有4个顶点且面积较大）
        if (points.size == 4 && Imgproc.contourArea(contour) > 1000) {
            // 画出矩形
            for (i in points.indices) {
                Imgproc.line(src, points[i], points[(i + 1) % points.size], Scalar(0.0, 255.0, 0.0), 4)
            }
        }
    }

    // 显示或保存结果图像
    Imgcodecs.imwrite("output_image.jpg", src)
}
