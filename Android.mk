LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := GSMBackend
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := GSMBackend

gsmback_root  := $(LOCAL_PATH)
gsmback_dir   := app
gsmback_out   := $(OUT_DIR)/target/common/obj/APPS/$(LOCAL_MODULE)_intermediates
gsmback_build := $(gsmback_root)/$(gsmback_dir)/build
gsmback_apk   := build/outputs/apk/$(gsmback_dir)-release-unsigned.apk

$(gsmback_root)/$(gsmback_dir)/$(gsmback_apk):
        rm -Rf $(gsmback_build)
        mkdir -p $(gsmback_out)
        ln -sf $(gsmback_out) $(gsmback_build)
        cd $(gsmback_root)/$(gsmback_dir) && JAVA_TOOL_OPTIONS="$(JAVA_TOOL_OPTI                                                                           ONS) -Dfile.encoding=UTF8" ../gradlew assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(gsmback_dir)/$(gsmback_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)
