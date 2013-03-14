LOCAL_PATH := $(call my-dir)

#used for compiling libraw
include $(CLEAR_VARS)
LOCAL_CFLAGS += -I$(SYSROOT)/usr/lib/include/libraw -pthread -w
LOCAL_CXXFLAGS += -I$(SYSROOT)/usr/lib/include/libraw -pthread -w
LOCAL_MODULE     := libraw					# name of your module
LOCAL_LDLIBS     += -L$(SYSROOT)/usr/lib -lstdc++ # libraries to link against, lstdc++ is auto-linked
LOCAL_SRC_FILES  :=  internal/dcraw_common.cpp internal/dcraw_fileio.cpp internal/demosaic_packs.cpp src/libraw_cxx.cpp src/libraw_c_api.cpp
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/libraw
include $(BUILD_STATIC_LIBRARY)

include librawutils/Android.mk
