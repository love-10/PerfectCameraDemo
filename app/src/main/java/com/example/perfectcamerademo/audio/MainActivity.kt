package com.example.perfectcamerademo.audio

import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import com.example.perfectcamerademo.App
import com.example.perfectcamerademo.App.Companion.instance
import com.example.perfectcamerademo.R
import com.zlw.main.recorderlib.RecordManager
import com.zlw.main.recorderlib.recorder.RecordConfig
import com.zlw.main.recorderlib.recorder.RecordHelper.RecordState
import com.zlw.main.recorderlib.recorder.listener.RecordStateListener
import org.example.SingleKalman.log
import java.util.Locale

class MainActivity : FragmentActivity(), AdapterView.OnItemSelectedListener, View.OnClickListener {
    var btRecord: Button? = null
    var btStop: Button? = null
    var tvState: TextView? = null
    var tvSoundSize: TextView? = null
    var rgAudioFormat: RadioGroup? = null
    var rgSimpleRate: RadioGroup? = null
    var tbEncoding: RadioGroup? = null
    var tbSource: RadioGroup? = null
    var audioView: AudioView? = null
    var spUpStyle: Spinner? = null
    var spDownStyle: Spinner? = null

    private var isStart = false
    private var isPause = false
    private val recordManager: RecordManager = RecordManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)
        initView()
        initAudioView()
        initEvent()
        initRecord()
    }

    private fun initView() {
        btRecord = findViewById(R.id.btRecord)
        btStop = findViewById(R.id.btStop)
        tvState = findViewById(R.id.tvState)
        btRecord = findViewById(R.id.btRecord)
        tvSoundSize = findViewById(R.id.tvSoundSize)
        rgAudioFormat = findViewById(R.id.rgAudioFormat)
        rgSimpleRate = findViewById(R.id.rgSimpleRate)
        tbEncoding = findViewById(R.id.tbEncoding)
        audioView = findViewById(R.id.audioView)
        spUpStyle = findViewById(R.id.spUpStyle)
        spDownStyle = findViewById(R.id.spDownStyle)
        tbSource = findViewById(R.id.tbSource)
        btRecord!!.setOnClickListener(this)
        btStop!!.setOnClickListener(this)
        findViewById<View>(R.id.jumpTestActivity).setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        initRecordEvent()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun initAudioView() {
        tvState!!.visibility = View.GONE
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, STYLE_DATA)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spUpStyle!!.adapter = adapter
        spDownStyle!!.adapter = adapter
        spUpStyle!!.onItemSelectedListener = this
        spDownStyle!!.onItemSelectedListener = this
    }

    private fun initEvent() {
        rgAudioFormat!!.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.rbPcm) {
                recordManager.changeFormat(RecordConfig.RecordFormat.PCM)
            } else if (checkedId == R.id.rbMp3) {
                recordManager.changeFormat(RecordConfig.RecordFormat.MP3)
            } else if (checkedId == R.id.rbWav) {
                recordManager.changeFormat(RecordConfig.RecordFormat.WAV)
            }
        }

        rgSimpleRate!!.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.rb8K) {
                recordManager.changeRecordConfig(recordManager.recordConfig.setSampleRate(8000))
            } else if (checkedId == R.id.rb16K) {
                recordManager.changeRecordConfig(recordManager.recordConfig.setSampleRate(16000))
            } else if (checkedId == R.id.rb44K) {
                recordManager.changeRecordConfig(recordManager.recordConfig.setSampleRate(44100))
            }
        }

        tbEncoding!!.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.rb8Bit) {
                recordManager.changeRecordConfig(
                    recordManager.recordConfig.setEncodingConfig(
                        AudioFormat.ENCODING_PCM_8BIT
                    )
                )
            } else if (checkedId == R.id.rb16Bit) {
                recordManager.changeRecordConfig(
                    recordManager.recordConfig.setEncodingConfig(
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                )
            }
        }
        tbSource!!.setOnCheckedChangeListener { group, checkedId -> }
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
                    RecordState.PAUSE -> tvState!!.text = "暂停中"
                    RecordState.IDLE -> tvState!!.text = "空闲中"
                    RecordState.RECORDING -> tvState!!.text = "录音中"
                    RecordState.STOP -> tvState!!.text = "停止"
                    RecordState.FINISH -> {
                        tvState!!.text = "录音结束"
                        tvSoundSize!!.text = "---"
                    }

                    else -> {}
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
        })
        recordManager.setRecordSoundSizeListener { soundSize ->
            tvSoundSize!!.text = String.format(
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
        recordManager.setRecordFftDataListener { data -> audioView!!.setWaveData(data) }
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
        btRecord!!.text = "开始"
        isPause = false
        isStart = false
    }

    private fun doPlay() {
        if (isStart) {
            recordManager.pause()
            btRecord!!.text = "开始"
            isPause = true
            isStart = false
        } else {
            if (isPause) {
                recordManager.resume()
            } else {
                recordManager.start()
            }
            btRecord!!.text = "暂停"
            isStart = true
        }
    }


    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val parentId = parent.id
        if (parentId == R.id.spUpStyle) {
            audioView!!.setStyle(
                AudioView.ShowStyle.getStyle(STYLE_DATA[position]),
                audioView!!.downStyle
            )
        } else if (parentId == R.id.spDownStyle) {
            audioView!!.setStyle(
                audioView!!.upStyle,
                AudioView.ShowStyle.getStyle(STYLE_DATA[position])
            )
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        //nothing
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName

        private val STYLE_DATA =
            arrayOf("STYLE_ALL", "STYLE_NOTHING", "STYLE_WAVE", "STYLE_HOLLOW_LUMP")
    }
}
