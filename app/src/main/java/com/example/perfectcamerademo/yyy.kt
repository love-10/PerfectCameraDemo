package com.example.perfectcamerademo


val yolo6 by lazy {
    Yolo6().apply {
        loadModel(App.instance!!.assets, 0, 0, true)
    }
}