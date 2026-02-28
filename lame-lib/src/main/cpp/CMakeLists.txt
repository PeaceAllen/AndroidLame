
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("mylibrary")

# 设置变量
set(LAME_DIR ${CMAKE_SOURCE_DIR}/../../../libs/lame/${ANDROID_ABI})
message("dir: ${LAME_DIR}")
include_directories(${LAME_DIR}/include)

add_library(lame-lib STATIC IMPORTED)
set_target_properties(lame-lib PROPERTIES IMPORTED_LOCATION
        ${LAME_DIR}/lib/libmp3lame.a)


add_library(${CMAKE_PROJECT_NAME} SHARED
    # List C/C++ source files with relative paths to this CMakeLists.txt.
    mylibrary.cpp)





# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
    android
    lame-lib
    log)
