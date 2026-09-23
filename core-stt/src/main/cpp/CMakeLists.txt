cmake_minimum_required(VERSION 3.22.1)
project(mangal_whisper)

# Same situation as core-llm/src/main/cpp/CMakeLists.txt: no prebuilt binary
# bundled. Add the upstream source once, locally:
#   cd core-stt/src/main/cpp
#   git submodule add https://github.com/ggerganov/whisper.cpp whisper.cpp
#   git submodule update --init --recursive

add_subdirectory(whisper.cpp whisper.cpp_build)

add_library(mangal_whisper SHARED
    whisper_jni.cpp
)

target_include_directories(mangal_whisper PRIVATE
    whisper.cpp/include
    whisper.cpp/ggml/include
)

target_link_libraries(mangal_whisper
    whisper
    android
    log
)
