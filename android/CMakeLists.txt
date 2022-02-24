cmake_minimum_required(VERSION 3.9.0)

set (PACKAGE_NAME "react-native-random-values-jsi-helper")
set (BUILD_DIR ${CMAKE_SOURCE_DIR}/build)
#set (CMAKE_CXX_FLAGS "-DFOLLY_NO_CONFIG=1 -DFOLLY_HAVE_CLOCK_GETTIME=1 -DFOLLY_HAVE_MEMRCHR=1 -DFOLLY_USE_LIBCPP=1 -DFOLLY_MOBILE=1")

#file (GLOB LIBFBJNI_INCLUDE_DIR "${BUILD_DIR}/fbjni-*-headers.jar/")

include_directories(
        "src/cpp"
        "${NODE_MODULES_DIR}/react-native/React"
        "${NODE_MODULES_DIR}/react-native/React/Base"
        "${NODE_MODULES_DIR}/react-native/ReactCommon/jsi"
        "${NODE_MODULES_DIR}/react-native/ReactAndroid/src/main/jni"
        "${NODE_MODULES_DIR}/react-native/ReactAndroid/src/main/java/com/facebook/react/turbomodule/core/jni"
)

if(${REACT_NATIVE_VERSION} LESS 66)
  set (
          INCLUDE_JSI_CPP
          "${NODE_MODULES_DIR}/react-native/ReactCommon/jsi/jsi/jsi.cpp"
  )
endif()

add_library(reactnativerandomvaluesjsihelper
        SHARED
        cpp-adapter.cpp
        ${INCLUDE_JSI_CPP}
)

#target_include_directories(
#        reactnativerandomvaluesjsihelper
#)

set_target_properties(
        reactnativerandomvaluesjsihelper PROPERTIES
        CXX_STANDARD 17
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
)

file (GLOB LIBRN_DIR "${PREBUILT_DIR}/${ANDROID_ABI}")

find_library(
        log-lib
        log
)
find_library(
        REACT_NATIVE_JNI_LIB
        reactnativejni
        PATHS ${LIBRN_DIR}
        NO_CMAKE_FIND_ROOT_PATH
)


target_link_libraries(
        reactnativerandomvaluesjsihelper
        ${log-lib}
        ${REACT_NATIVE_JNI_LIB}
        android
)
