package com.example.flirapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.CountDownTimer;

import java.io.File;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoHandler {
    private int finishStatus;
    private final String filePathName;
    private BitmapToVideoEncoder bitmapToVideoEncoder;
    private LinkedBlockingQueue<Bitmap> bitmapQueue = new LinkedBlockingQueue<>(200);
    //    private boolean countDownFinished;
    private int videoCount = 0;
    private int noEqualToTmp = 0;

    public VideoHandler(String filePathName) {
        this.finishStatus = 0;
        this.filePathName = filePathName;
    }

    //    protected void init(int tmp){
//        if (noEqualToTmp!=tmp){
//            taskQueue();
//            noEqualToTmp=tmp;
//        }
//    }
    protected void init() {
            taskQueue();
    }

    private void taskQueue() {
        int bitmapCount = 0;
        while (!bitmapQueue.isEmpty()) {
//            timerStart();
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
            bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
                @Override
                public void onEncodingComplete(File outputFile) {
                }
            });
            Bitmap bitmap = bitmapQueue.poll();
            if (bitmap == null) {
                bitmapToVideoEncoder.stopEncoding();
            } else {
                bitmapToVideoEncoder.startEncoding(bitmap.getWidth(), bitmap.getHeight(), new File(filePathName + "." + videoCount));
                videoCount++;
                while (bitmapCount < 1800 && bitmap != null) {
                    bitmapCount++;
                    bitmapToVideoEncoder.queueFrame(bitmap);
                    bitmap = bitmapQueue.poll();
                }
                bitmapToVideoEncoder.stopEncoding();
                bitmapCount = 0;
            }
//                }
//            }).start();
        }
    }


    protected void pass(Bitmap bitmap) throws InterruptedException {
        bitmapQueue.put(bitmap);
    }

    protected int finished() {
        while (true) {
            if (bitmapQueue.isEmpty()) {
                this.finishStatus = 1;
                return merge();
            }
        }
    }

    private int merge() {
//        for (int i=0;i<videoCount;i++){
//
//        }
        return 0;
    }

//    private final CountDownTimer itimer = new CountDownTimer(5 * 1000, 1000) {
//        @Override
//        public void onTick(long millisUntilFinished) {
//        }
//
//        @Override
//        public void onFinish() {
//            countDownFinished = true;
//        }
//    };
//
//    private void timerCancel() {
//        itimer.cancel();
//    }
//
//    private void timerStart() {
//        countDownFinished=false;
//        itimer.start();
//    }

}
