package com.senseforce.rawonandroid;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.senseforce.rawonandroid.raw.RawUtils;

import java.io.File;

public class DemoActivity extends Activity implements Handler.Callback, View.OnClickListener {

    private static int MSG_THUMB_SHOWN = 1;
    private static int MSG_TEST_INTERVAL = 2;
    private static int MSG_TEST_OVER = 3;

    private TimeChecker t = TimeChecker.getInstance();

    private ImageView thumbView = null;
    private TextView hintTextView = null;
    private Button testButton = null;
    private TextView consoleTextView = null;

    private Handler mHandler = null;
    private StringBuilder consoleString = new StringBuilder();
    private StringBuilder tempString = new StringBuilder();

    private String sdcardFilepath = Environment.getExternalStorageDirectory().getPath() + "/";
    private String cr2FilePath = sdcardFilepath + "raw.CR2";
    private String nefFilePath = sdcardFilepath + "raw.NEF";
    private String jpgFilePath = sdcardFilepath + "raw.jpg";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo);

        thumbView = (ImageView)this.findViewById(R.id.imageView);
        hintTextView = (TextView)this.findViewById(R.id.textView);
        testButton = (Button)this.findViewById(R.id.button);
        consoleTextView = (TextView)this.findViewById(R.id.console);

        testButton.setEnabled(false);
        testButton.setOnClickListener(this);

        mHandler =new Handler(this);
        if ( ! new File(cr2FilePath).exists()) {
           hintTextView.setText("no raw file");
        }
        else {
            new Thread(new Runnable () {

                @Override
                public void run() {
                    Bitmap testThumbnailBitmap = RawUtils.unpackThumbnailBitmapToFit(cr2FilePath, 256, 256);

                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_THUMB_SHOWN;
                    msg.obj = testThumbnailBitmap;
                    mHandler.sendMessage(msg);
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

    private void notifyInterval(String log) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_TEST_INTERVAL;
        msg.obj = log;
        mHandler.sendMessage(msg);
    }

    private void checkAndNotify(String tag) {
        long interval = t.check(tag);

        notifyInterval(tempString.delete(0, tempString.length()).append(tag).append(" 耗时 ").append(interval).append(" 毫秒").toString());
    }

    @Override
    public void onClick(View view) {
        testButton.setEnabled(false);
        hintTextView.setText("testing");

        new Thread(new Runnable () {

            @Override
            public void run() {

                testRawFile(cr2FilePath);

                Message msg = mHandler.obtainMessage();
                msg.what = MSG_TEST_OVER;
                mHandler.sendMessage(msg);
            }
        }).start();

    }

    private void testRawFile(String rawFileName) {
        File rawFile = new File(rawFileName);
        if ( ! rawFile.exists()) {
            return;
        }

        notifyInterval("\n测试" + (rawFile.length()/(1024*1024)) + "M大小的" + rawFileName);

        t.prepare();
        RawUtils.saveThumbnailToFile(cr2FilePath, sdcardFilepath + "cr2_big.jpg");
        DemoActivity.this.checkAndNotify("提取原大小图片到SD卡");

        t.prepare();
        RawUtils.saveThumbnailToFitToFile(cr2FilePath, sdcardFilepath + "cr2_720.jpg", 720, 1280);
        DemoActivity.this.checkAndNotify("保存全屏大小缩略图到SD卡");

        t.prepare();
        RawUtils.saveThumbnailToFitToFile(cr2FilePath, sdcardFilepath + "cr2_300.jpg", 300, 300);
        DemoActivity.this.checkAndNotify("保存300px缩略图到SD卡");

        t.prepare();
        RawUtils.saveThumbnailToFitToFile(cr2FilePath, sdcardFilepath + "cr2_100.jpg", 100, 100);
        DemoActivity.this.checkAndNotify("保存100px缩略图到SD卡");
    }

    @Override
    public boolean handleMessage(Message msg) {
        // TODO Auto-generated method stub
        if (msg.what == MSG_THUMB_SHOWN) {
            thumbView.setImageDrawable(new BitmapDrawable(this.getResources(), (Bitmap)msg.obj));

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
