package com.senseforce.rawonandroid.raw;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.util.Log;
import com.senseforce.rawonandroid.TimeChecker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RawUtils {

    private static int DEFAULT_JPG_QUALITY = 85;

    private RawUtils() {

    }

    static {
        try {
            System.loadLibrary("rawutils");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

	/*
	 * Native函数
	 */

    private static native byte[] unpackThumbnailBytes(String fileName);

    private static native int unpackThumbnailToFile(String rawFileName, String thumbFileName);


    /**
     * 获取缩略图
     * @param fileName
     * @param height
     * @param width
     * @return
     */
    public static Bitmap unpackThumbnailBitmapToFit(String fileName, int width, int height) {
        TimeChecker t = TimeChecker.newInstance();
        t.prepare();
        Bitmap thumbnail;
        byte[] thumbnailBytes = unpackThumbnailBytes(fileName);
        t.check("从jni读入原大小缩略图字节");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length, options);
        options.inJustDecodeBounds = false;

        int originHeight = options.outHeight;
        int originWidth = options.outWidth;

        int scaleWidth = (int) Math.ceil(originWidth / (float) width);
        int scaleHeight = (int) Math.ceil(originHeight / (float) height);
        int scale = (scaleWidth < scaleHeight ? scaleWidth : scaleHeight);

        options.inSampleSize = scale;
        t.check("获取尺寸并计算缩放倍数");

        thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length, options);
        t.check("缩放位图");

        System.gc();

        return thumbnail;
    }

    /**
     * 将raw中的缩略图保存至文件
     * @param rawFileName
     * @param thumbFileName
     * @return
     */
    public static int saveThumbnailToFile(String rawFileName, String thumbFileName) {
        return  unpackThumbnailToFile(rawFileName, thumbFileName);
    }

    public static boolean saveThumbnailToFitToFile(String rawFileName, String scaledFileName, int width, int height) {
        TimeChecker t = TimeChecker.newInstance();
        t.prepare();
        Bitmap bitmap = unpackThumbnailBitmapToFit(rawFileName, width, height);
        t.check("读取缩略图并改尺寸");
        boolean result = compressBitmapAndSave(bitmap, scaledFileName);
        t.check("压缩为JPG");
        return result;
    }

    public static boolean scaleJPGAndSave(String originFileName, String scaledFileName, int width, int height) {

        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(originFileName, options);
        options.inJustDecodeBounds = false;
        int originHeight = options.outHeight;
        int originWidth = options.outWidth;

        int scaleWidth = (int) Math.ceil(originWidth / (float) width);
        int scaleHeight = (int) Math.ceil(originHeight / (float) height);
        int scale = (scaleWidth < scaleHeight ? scaleWidth : scaleHeight);

        if (scale == 0) {
            return false;
        }

        options.inSampleSize = scale;
        bitmap = BitmapFactory.decodeFile(originFileName, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, originWidth / scale, originHeight / scale,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        return compressBitmapAndSave(bitmap, scaledFileName);
    }

    public static boolean compressBitmapAndSave(Bitmap bitmap, String savedFileName) {
        boolean result = false;

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(savedFileName);

            if (bitmap != null) {
                result = bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_JPG_QUALITY, outputStream);
            }
            outputStream.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return result;
    }


    public static ExifInterface parseExif(String fileName) {
        ExifInterface oldExif = null;
        try {
            oldExif = new ExifInterface(fileName);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (oldExif != null) {
            Log.d("RAW", "parsed image height = " + oldExif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            try {
                oldExif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return oldExif;
    }


}
