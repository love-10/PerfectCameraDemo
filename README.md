import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName().toString();
            Log.d("MyAccessibilityService", "Current app package: " + packageName);

            PackageManager pm = getPackageManager();
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String appName = pm.getApplicationLabel(appInfo).toString();
                Drawable appIcon = pm.getApplicationIcon(appInfo);

                Log.d("MyAccessibilityService", "Current app name: " + appName);
                // 你可以在这里使用appIcon，比如显示在某个ImageView中
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onInterrupt() {
        // 处理中断的逻辑
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        setServiceInfo(info);
    }
}



import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.onnxruntime.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 从 assets 目录加载模型并进行推理
        val modelBytes = loadModelFile(this, "model.onnx")
        if (modelBytes != null) {
            try {
                val env = OrtEnvironment.getEnvironment()
                val session = env.createSession(modelBytes)

                // 创建输入数据（根据你的模型输入调整）
                val inputName = session.inputNames.iterator().next()
                val inputTensor = OnnxTensor.createTensor(env, floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f), longArrayOf(1, 4))

                // 进行推理
                val result = session.run(Collections.singletonMap(inputName, inputTensor))
                val outputTensor = result[0] as OnnxTensor
                val outputArray = outputTensor.floatBuffer.array()

                // 输出结果
                Log.d("ONNX", "Inference result: ${outputArray.joinToString()}")
            } catch (e: OrtException) {
                e.printStackTrace()
            }
        }
    }

    // 从 assets 目录加载模型文件
    private fun loadModelFile(context: Context, modelName: String): ByteArray? {
        return try {
            val inputStream = context.assets.open(modelName)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            buffer
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
