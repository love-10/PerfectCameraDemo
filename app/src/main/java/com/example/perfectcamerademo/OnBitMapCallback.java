package com.example.perfectcamerademo;

import android.graphics.Bitmap;

import java.util.ArrayList;

public interface OnBitMapCallback {
    void onBitmap(Bitmap bitmap, ArrayList<Box> boxes);
}
