#include <jni.h>
#include <android/log.h>         /*引入头文件*/
#include <stdlib.h>
#include <stdio.h>

#include "../libraw/libraw.h"

#define TAG_DEBUG "RAW"  		/*宏定义（自定义)*/


extern "C" JNIEXPORT void JNICALL Java_com_senseforce_rawonandroid_raw_RawUtils_native_init(JNIEnv * env) {

}

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_senseforce_rawonandroid_raw_RawUtils_unpackThumbnailBytes(JNIEnv * env, jobject obj, jstring jfilename)
{
	LibRaw raw;

    __android_log_print(ANDROID_LOG_DEBUG, TAG_DEBUG, "raw unpackThumbnailBytes");
	jboolean bIsCopy;
	const char* strFilename = (env)->GetStringUTFChars(jfilename , &bIsCopy);

	// Open the file and read the metadata
	raw.open_file(strFilename);
	__android_log_print(ANDROID_LOG_DEBUG, TAG_DEBUG, "raw file : %c", strFilename);


	(env)->ReleaseStringUTFChars(jfilename, strFilename);// release jstring

	// Let us unpack the image
	raw.unpack_thumb();

	jsize length = raw.imgdata.thumbnail.tlength;

	jbyteArray jb = (env)->NewByteArray(length);
	env->SetByteArrayRegion(jb,0,length,(jbyte *)raw.imgdata.thumbnail.thumb);

	// Finally, let us free the image processor for work with the next image
	raw.recycle();
	return jb;
}

extern "C" JNIEXPORT jint JNICALL Java_com_senseforce_rawonandroid_raw_RawUtils_unpackThumbnailToFile(JNIEnv * env, jobject obj, jstring jrawfilename, jstring jthumbfilename) {
    LibRaw raw;

	jboolean bIsCopy;
	const char* strRawFilename = (env)->GetStringUTFChars(jrawfilename , &bIsCopy);
	const char* strThumbFilename = (env)->GetStringUTFChars(jthumbfilename , &bIsCopy);

	// Open the file and read the metadata
	raw.open_file(strRawFilename);

	// Let us unpack the image
	raw.unpack_thumb();

	jint ret = raw.dcraw_thumb_writer(strThumbFilename);

	(env)->ReleaseStringUTFChars(jrawfilename, strRawFilename);// release jstring
	(env)->ReleaseStringUTFChars(jthumbfilename, strThumbFilename);// release jstring

	// Finally, let us free the image processor for work with the next image
	raw.recycle();
    return ret;
}

extern "C" JNIEXPORT void JNICALL Java_com_senseforce_rawonandroid_raw_RawUtils_parseExif(JNIEnv * env, jobject obj, jstring jfilename, jobject jexifMap) {

//    jclass ExifInterface = (env)->FindClass("android/media/ExifInterface");
//    jmethodID SetAttributeMethod = (env)->GetMethodID(ExifInterface, "setAttribute", "(Ljava/lang/String;Ljava/lang/String;)V");

    jclass classHashMap = (env)->FindClass("java/util/HashMap");
    jmethodID methodPut = (env)->GetMethodID(classHashMap, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    LibRaw raw;

	jboolean bIsCopy;

	const char* strFilename = (env)->GetStringUTFChars(jfilename , &bIsCopy);

	raw.open_file(strFilename);

	(env)->ReleaseStringUTFChars(jfilename, strFilename);

    char buf[15];

    sprintf(buf, "%.0f", raw.imgdata.other.iso_speed);
	(env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("ISOSpeedRatings"), (env)->NewStringUTF(buf));

    sprintf(buf, "%u", raw.imgdata.sizes.iwidth);
	(env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("ImageWidth"), (env)->NewStringUTF(buf));

	sprintf(buf, "%u", raw.imgdata.sizes.iheight);
    (env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("ImageLength"), (env)->NewStringUTF(buf));

    sprintf(buf, "%ld", raw.imgdata.other.timestamp);
    (env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("DateTime"), (env)->NewStringUTF(buf));

    sprintf(buf, "%f", raw.imgdata.other.shutter);
    (env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("Shutter"), (env)->NewStringUTF(buf));

    sprintf(buf, "%f", raw.imgdata.other.aperture);
    (env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("FNumber"), (env)->NewStringUTF(buf));

    sprintf(buf, "%d", raw.imgdata.sizes.flip);
    (env)->CallObjectMethod(jexifMap, methodPut, (env)->NewStringUTF("Orientation"), (env)->NewStringUTF(buf));

	raw.recycle();
}