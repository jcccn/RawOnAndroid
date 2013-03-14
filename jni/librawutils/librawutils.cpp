#include <jni.h>
#include <android/log.h>         /*引入头文件*/

#include "../libraw/libraw.h"

#define TAG_DEBUG "RAW"  		/*宏定义（自定义)*/

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