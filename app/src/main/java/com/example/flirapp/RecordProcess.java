package com.example.flirapp;
// CameraDetected is main activity

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public class RecordProcess extends AppCompatActivity {
    private static final String TAG = "RecordProcess";

    //Handles Android permission for eg Network
    private PermissionHandler permissionHandler;

    //Handles network camera operations
    private CameraHandler cameraHandler;

    private Identity connectedIdentity = null;
    private TextView status;
    private ImageButton captureButton;
    private ImageView msxImage;
    //    private ImageView photoImage;
    private FrameDataHolder currentDataHolder; //for capture
    private Bitmap currentMsxBitmap;//for capture
    //    private Bitmap currentDcBitmap;//for capture
    private LinkedBlockingQueue<Bitmap> currentFramesBuffer = new LinkedBlockingQueue<>(100); //for video
    private boolean videoRecordFinished;


    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue<>(21);
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_process);

//        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;

        //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
        // and before ANY using any ThermalSdkAndroid functions
        //ThermalLog will show log from the Thermal SDK in standards android log framework
//        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);

        permissionHandler = new PermissionHandler(showMessage, RecordProcess.this);
        cameraHandler = CameraDetected.cameraHandler;
        setupViews();

        permissionHandler.checkForStoragePermission();

        new Thread(new Runnable() {
            @Override
            public void run() {
                cameraHandler.startStream(streamDataListener);
            }
        }).start();
    }


//    public void startDiscovery(View view) {
//        startDiscovery();
//    }
//
//    public void stopDiscovery(View view) {
//        stopDiscovery();
//    }


    public void connect(View view) {
//        connect(cameraHandler.getFlirOne());
        connect(cameraHandler.getFlirOneEmulator());

    }


    public void disconnect(View view) {
        disconnect();
    }

    /**
     * Handle Android permission request response for Bluetooth permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + permissions + "], grantResults = [" + grantResults + "]");
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Connect to a Camera
     */
    private void connect(Identity identity) {
        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if (identity == null) {
            Log.d(TAG, "connect(), can't connect, no camera available");
            showMessage.show("connect(), can't connect, no camera available");
            return;
        }

        connectedIdentity = identity;

        //IF your using "USB_DEVICE_ATTACHED" and "usb-device vendor-id" in the Android Manifest
        // you don't need to request permission, see documentation for more information
        if (UsbPermissionHandler.isFlirOne(identity)) {
            usbPermissionHandler.requestFlirOnePermisson(identity, this, permissionListener);
        }
    }

    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
            doConnect(identity);
        }

        @Override
        public void permissionDenied(Identity identity) {
//            RecordProcess.this.showMessage.show("Permission was denied for identity ");
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
//            RecordProcess.this.showMessage.show("Error when asking for permission for FLIR ONE, error:"+errorType+ " identity:" +identity);
        }
    };

    private void doConnect(Identity identity) {
        new Thread(() -> {
            try {
                cameraHandler.connect(identity, connectionStatusListener);
                runOnUiThread(() -> {
                    cameraHandler.startStream(streamDataListener);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Could not connect: " + e);
//                    updateConnectionText(identity, "DISCONNECTED");
                });
            }
        }).start();
    }

    /**
     * Disconnect to a camera
     */
    private void disconnect() {
//        updateConnectionText(connectedIdentity, "DISCONNECTING");
        connectedIdentity = null;
        Log.d(TAG, "disconnect() called with: connectedIdentity = [" + connectedIdentity + "]");
        new Thread(() -> {
            cameraHandler.disconnect();
//            runOnUiThread(() -> {
//                updateConnectionText(null, "DISCONNECTED");
//            });
        }).start();
    }

    /**
     * Update the UI text for connection status
     */
//    private void updateConnectionText(Identity identity, String status) {
//        String deviceId = identity != null ? identity.deviceId : "";
//        connectionStatus.setText(getString(R.string.connection_status_text, deviceId + " " + status));
//    }

    /**
     * Start camera discovery
     */
//    private void startDiscovery() {
//        cameraHandler.startDiscovery(cameraDiscoveryListener, discoveryStatusListener);
//    }

    /**
     * Stop camera discovery
     */
//    private void stopDiscovery() {
//        cameraHandler.stopDiscovery(discoveryStatusListener);
//    }

    /**
     * Callback for discovery status, using it to update UI
     */
//    private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
//        @Override
//        public void started() {
//            discoveryStatus.setText(getString(R.string.connection_status_text, "discovering"));
//
//        }
//
//        @Override
//        public void stopped() {
//            discoveryStatus.setText(getString(R.string.connection_status_text, "not discovering"));
////            Intent intent = new Intent();
////            intent.setClass(MainActivity.this, failure.class);
////            startActivity(intent);
//        }
//    };

    /**
     * Camera connecting state thermalImageStreamListener, keeps track of if the camera is connected or not
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onDisconnected(@org.jetbrains.annotations.Nullable ErrorCode errorCode) {
            Log.d(TAG, "onDisconnected errorCode:" + errorCode);

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                    updateConnectionText(connectedIdentity, "DISCONNECTED");
//                }
//            });
        }
    };

    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {
            currentDataHolder = dataHolder;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    msxImage.setImageBitmap(dataHolder.msxBitmap);
//                    photoImage.setImageBitmap(dataHolder.dcBitmap);
                }
            });
        }

        @Override
        public void images(Bitmap msxBitmap, Bitmap dcBitmap) {
//            currentDcBitmap = dcBitmap;
            currentMsxBitmap = msxBitmap;
            try {
                framesBuffer.put(new FrameDataHolder(msxBitmap, dcBitmap));
            } catch (InterruptedException e) {
                //if interrupted while waiting for adding a new item in the queue
                Log.e(TAG, "images(), unable to add incoming images to frames buffer, exception:" + e);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "framebuffer size:" + framesBuffer.size());
                    FrameDataHolder poll = framesBuffer.poll();
                    msxImage.setImageBitmap(poll.msxBitmap);
//                    photoImage.setImageBitmap(poll.dcBitmap);
                }
            });
//            capture();
        }
    };

    /**
     * Camera Discovery thermalImageStreamListener, is notified if a new camera was found during a active discovery phase
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
//    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
//        @Override
//        public void onCameraFound(Identity identity) {
//            Log.d(TAG, "onCameraFound identity:" + identity);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    cameraHandler.add(identity);
//                }
//            });
//        }
//
//        @Override
//        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
//            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
//
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    stopDiscovery();
//                    RecordProcess.this.showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
//                }
//            });
//        }
//    };

    private ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(RecordProcess.this, message, Toast.LENGTH_SHORT).show();
        }
    };


    private void setupViews() {
        status = findViewById(R.id.status);
//        captureButton = findViewById(R.id.imageButton);
        msxImage = findViewById(R.id.msx_image);
//        photoImage = findViewById(R.id.photo_image);
    }


    public void capture(View view) {
        new Thread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/flirapp/image/";
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String state = Environment.getExternalStorageState();
                if (state.equals(Environment.MEDIA_MOUNTED)) {
                    Calendar now = new GregorianCalendar();
                    SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmssSS", Locale.getDefault());
                    String fileName = simpleDate.format(now.getTime());
                    try {
                        File msxfile = new File(dirPath + fileName + "msxbitmap.jpg");
//                        File dcfile=new File(dirPath + fileName + "dcbitmap.jpg");
                        FileOutputStream msxout = new FileOutputStream(msxfile);
//                        FileOutputStream dcout = new FileOutputStream(dcfile);
                        currentMsxBitmap.compress(Bitmap.CompressFormat.JPEG, 100, msxout);
//                        currentDcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, dcout);
                        msxout.flush();
                        msxout.close();
//                        dcout.flush();
//                        dcout.close();
                        runOnUiThread(() -> {
                            showMessage.show("Saved");
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            showMessage.show("Save failed! " + e);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showMessage.show("Save failed! Media is not mounted.");
                    });
                }
            }
        }).start();
    }


    @SuppressLint("SetTextI18n")
    public void video(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/flirapp/image/";
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String state = Environment.getExternalStorageState();
                if (state.equals(Environment.MEDIA_MOUNTED)) {
                    Calendar now = new GregorianCalendar();
                    SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                    String fileName = simpleDate.format(now.getTime());
                    videoRecordFinished = false;
                    timerStart();
                    try {
                        int result = videoHandler(dirPath + fileName + ".mp4");
                        runOnUiThread(() -> {
                            showMessage.show("Saved.");
                        });
                    } catch (InterruptedException e) {
                        runOnUiThread(() -> {
                            showMessage.show("Save failed. " + e);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showMessage.show("Save failed! Media is not mounted.");
                    });
                }
            }
        }).start();
    }


    private int videoHandler(String filePathName) throws InterruptedException {
        VideoHandler videoHandler = new VideoHandler(filePathName);
        int count = 0,inited=0;
        while (!videoRecordFinished) {
            if (!currentMsxBitmap.isRecycled()) {
                Bitmap tmp = currentMsxBitmap.copy(currentMsxBitmap.getConfig(), false);
                videoHandler.pass(tmp);
                count++;
                if (count > 100 && inited==0) {
                    inited=1;
                    videoHandler.init();
                }
            }
        }
        runOnUiThread(() -> {
            showMessage.show("Preparing file...");
        });
        return videoHandler.finished();
    }


    private final CountDownTimer itimer = new CountDownTimer(30 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            runOnUiThread(() -> {
                status.setText(formatTime(millisUntilFinished));
            });
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onFinish() {
            videoRecordFinished = true;
            runOnUiThread(() -> {
                status.setText("00:00");
            });
        }
    };


    public String formatTime(long millisecond) {
        int minute;
        int second;
        minute = (int) ((millisecond / 1000) / 60);
        second = (int) ((millisecond / 1000) % 60);
        if (minute < 10) {
            if (second < 10) {
                return "0" + minute + ":" + "0" + second;
            } else {
                return "0" + minute + ":" + second;
            }
        } else {
            if (second < 10) {
                return minute + ":" + "0" + second;
            } else {
                return minute + ":" + second;
            }
        }
    }

    public void timerCancel() {
        itimer.cancel();
    }

    public void timerStart() {
        itimer.start();
    }


}

