package com.example.perfectcamerademo.audio

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.perfectcamerademo.App.Companion.instance
import com.example.perfectcamerademo.R
import com.example.perfectcamerademo.databinding.ActivityAudioBinding
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordHelper.RecordState
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener
import java.util.Locale

class MainActivity : FragmentActivity(), View.OnClickListener {

    private var isStart = false
    private var isPause = false
    private val recordManager: RecordManager = RecordManager.getInstance()
    private lateinit var binding: ActivityAudioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initRecord()
    }

    private fun initView() {
        binding.btRecord.setOnClickListener(this)
        binding.btStop.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        initRecordEvent()
    }

    private fun initRecord() {
        recordManager.init(instance, true)
        recordManager.changeFormat(RecordConfig.RecordFormat.MP3)
        recordManager.changeRecordDir(getExternalFilesDir("Record")!!.path)
        initRecordEvent()
    }

    private fun initRecordEvent() {
        recordManager.setRecordStateListener(object : RecordStateListener {
            override fun onStateChange(state: RecordState) {
                when (state) {
                    RecordState.PAUSE -> binding.tvState.text = "暂停中"
                    RecordState.IDLE -> binding.tvState.text = "空闲中"
                    RecordState.RECORDING -> binding.tvState.text = "录音中"
                    RecordState.STOP -> binding.tvState.text = "停止"
                    RecordState.FINISH -> {
                        binding.tvState.text = "录音结束"
                        binding.tvSoundSize.text = "---"
                    }

                    else -> {}
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
        })
        recordManager.setRecordSoundSizeListener { soundSize ->
            binding.tvSoundSize.text = String.format(
                Locale.getDefault(), "声音大小：%s db", soundSize
            )
        }
        recordManager.setRecordResultListener { result ->
            Toast.makeText(
                this@MainActivity,
                "录音文件： " + result.absolutePath,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.btRecord) {
            doPlay()
        } else if (id == R.id.btStop) {
            doStop()
        }
    }

    private fun doStop() {
        recordManager.stop()
        binding.btRecord.text = "开始"
        isPause = false
        isStart = false
    }

    private fun doPlay() {
        if (isStart) {
            recordManager.pause()
            binding.btRecord.text = "开始"
            isPause = true
            isStart = false
        } else {
            if (isPause) {
                recordManager.resume()
            } else {
                recordManager.start()
            }
            binding.btRecord.text = "暂停"
            isStart = true
        }
    }
}
