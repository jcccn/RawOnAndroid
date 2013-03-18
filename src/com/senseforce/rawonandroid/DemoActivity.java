package com.senseforce.rawonandroid;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.senseforce.rawonandroid.raw.RawUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class DemoActivity extends Activity implements Handler.Callback, View.OnClickListener {

    private static int MSG_THUMB_SHOWN = 1;
    private static int MSG_TEST_INTERVAL = 2;
    private static int MSG_TEST_OVER = 3;

    private TimeChecker t = TimeChecker.getInstance();

    private ImageView thumbView = null;
    private TextView hintTextView = null;
    private Button testButton = null;
    private Button clearButton = null;
    private TextView consoleTextView = null;

    private Handler mHandler = null;
    private StringBuilder consoleString = new StringBuilder();
    private StringBuilder tempString = new StringBuilder();

    private String sdcardFilepath = Environment.getExternalStorageDirectory().getPath() + "/";
    private String rawDirectoryPath = sdcardFilepath + "raw/";
    private String outputDirectoryPath = sdcardFilepath + "raw_output/";
    private String cr2FilePath = rawDirectoryPath + "CANON.CR2";
    private String nefFilePath = sdcardFilepath + "NIKON.NEF";
    private String jpgFilePath = sdcardFilepath + "raw.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo);

        thumbView = (ImageView)this.findViewById(R.id.imageView);
        hintTextView = (TextView)this.findViewById(R.id.textView);
        testButton = (Button)this.findViewById(R.id.button);
        clearButton = (Button)this.findViewById(R.id.button_clear);
        consoleTextView = (TextView)this.findViewById(R.id.console);

        testButton.setEnabled(false);
        testButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);

        mHandler =new Handler(this);
        if ( ! new File(cr2FilePath).exists()) {
            hintTextView.setText("no raw file");
        }
        else {
            new Thread(new Runnable () {

                @Override
                public void run() {
                    boolean result = RawUtils.saveThumbnailToFitToFile(cr2FilePath, outputDirectoryPath + "thumb.jpg", 256, 256);
                    if (result) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = MSG_THUMB_SHOWN;
                        msg.obj = outputDirectoryPath + "thumb.jpg";
                        mHandler.sendMessage(msg);
                    }
                }
            }).start();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.demo, menu);
        return true;
    }

    private void showLog(String log) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_TEST_INTERVAL;
        msg.obj = log;
        mHandler.sendMessage(msg);
    }

    private void checkAndNotify(String tag) {
        long interval = t.check(tag);

        showLog(tempString.delete(0, tempString.length()).append(tag).append(" 耗时 ").append(interval).append(" 毫秒").toString());
    }

    private void checkAndNotify(String tag, boolean success) {
        if ( ! success) {
            showLog(tempString.delete(0, tempString.length()).append(tag).append(" : 失败").toString());
        }
        else {
            long interval = t.check(tag);

            showLog(tempString.delete(0, tempString.length()).append(tag).append(" 耗时 ").append(interval).append(" 毫秒").toString());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button) {
            testButton.setEnabled(false);
            hintTextView.setText("testing");

            new Thread(new Runnable () {

                @Override
                public void run() {

                    File rawDirectory = new File(rawDirectoryPath);
                    File outputDirectory = new File(outputDirectoryPath);
                    if ( ! outputDirectory.exists()) {
                        outputDirectory.mkdirs();
                    }
                    for (File rawFile : rawDirectory.listFiles()) {
                        String filePath = rawFile.getPath();
                        testRawFile(filePath);
                    }

                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_TEST_OVER;
                    mHandler.sendMessage(msg);
                }
            }).start();
        }
        else if (view.getId() == R.id.button_clear) {
            consoleString.delete(0, consoleString.length());
            consoleTextView.setText("");
        }

    }

    private void testRawFile(String rawFileName) {
        File rawFile = new File(rawFileName);
        if ( ! rawFile.exists()) {
            return;
        }

        boolean isSuccessful = false;

        showLog("\n测试" + (rawFile.length() / (1024 * 1024)) + "M大小的" + rawFileName);
        t.prepare();
        HashMap exif = RawUtils.parseExif(rawFileName, false);
        DemoActivity.this.checkAndNotify("提取图片信息");
        showLog("原图宽度:" + exif.get(ExifInterface.TAG_IMAGE_WIDTH));
        showLog("原图高度:" + exif.get(ExifInterface.TAG_IMAGE_LENGTH));
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        showLog("拍摄时间:" + sdf.format(1000 * Long.valueOf((String) exif.get(ExifInterface.TAG_DATETIME))));
        showLog("ISO:" + exif.get(ExifInterface.TAG_ISO));
        showLog("快门:" + exif.get("Shutter"));
        showLog("光圈:" + exif.get(ExifInterface.TAG_APERTURE));
        int rotation = Integer.valueOf((String)exif.get(ExifInterface.TAG_ORIENTATION));
        String rotationString;
        switch (rotation) {
            case 0:
                rotationString = "正常";
                break;
            case 3:
                rotationString = "需要旋转180度";
                break;
            case 5:
                rotationString = "需要顺时针旋转270度";
                break;
            case 6:
                rotationString = "需要顺时针旋转90度";
                break;
            default:
                rotationString = "未知角度";
        }
        showLog("影像方向:" + rotationString);

        t.prepare();
        isSuccessful = RawUtils.saveThumbnailToFile(rawFileName, rawFileName.replace(rawDirectoryPath, outputDirectoryPath) + "_big.jpg");
        DemoActivity.this.checkAndNotify("提取原大小图片到SD卡", isSuccessful);

        t.prepare();
        isSuccessful = RawUtils.saveThumbnailToFitToFile(rawFileName, rawFileName.replace(rawDirectoryPath, outputDirectoryPath) + "_720.jpg", 720, 1280);
        DemoActivity.this.checkAndNotify("保存全屏大小缩略图到SD卡", isSuccessful);

        t.prepare();
        isSuccessful = RawUtils.saveThumbnailToFitToFile(rawFileName, rawFileName.replace(rawDirectoryPath, outputDirectoryPath) + "_300.jpg", 300, 300);
        DemoActivity.this.checkAndNotify("保存300px缩略图到SD卡", isSuccessful);

        t.prepare();
        isSuccessful = RawUtils.saveThumbnailToFitToFile(rawFileName, rawFileName.replace(rawDirectoryPath, outputDirectoryPath) + "_100.jpg", 100, 100);
        DemoActivity.this.checkAndNotify("保存100px缩略图到SD卡", isSuccessful);
    }

    @Override
    public boolean handleMessage(Message msg) {
        // TODO Auto-generated method stub
        if (msg.what == MSG_THUMB_SHOWN) {
            thumbView.setImageDrawable(new BitmapDrawable(this.getResources(), BitmapFactory.decodeFile((String)msg.obj)));
            thumbView.setBackgroundDrawable(null);

            testButton.setEnabled(true);
        }
        else if (msg.what == MSG_TEST_OVER) {
            testButton.setEnabled(true);
            hintTextView.setText("test over");
        }
        else if (msg.what == MSG_TEST_INTERVAL) {
            consoleString.append(msg.obj).append("\n");
            consoleTextView.setText(consoleString);
        }
        return false;
    }
}
