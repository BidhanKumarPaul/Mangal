cmake_minimum_required(VERSION 3.22.1)
project(mangal_llama)

# === READ THIS BEFORE BUILDING ===
# This module intentionally does NOT bundle a prebuilt llama.cpp .so or AAR.
# When I looked for a "maintained Android AAR" for llama.cpp, the top search
# results were recently-published packages from a single unverified
# maintainer with a paid "Pro tier" gating basic features (streaming, JSON
# grammar) behind a license — exactly the kind of native binary blob your own
# spec said to check with you before pulling in. I didn't add it.
#
# Instead, do this once, locally (I can't run git/network in my sandbox):
#   cd core-llm/src/main/cpp
#   git submodule add https://github.com/ggerganov/llama.cpp llama.cpp
#   git submodule update --init --recursive
# That's the upstream, MIT-licensed, auditable source — same one the whole
# ecosystem builds on. Then this CMakeLists will pick it up below.

add_subdirectory(llama.cpp llama.cpp_build)

add_library(mangal_llama SHARED
    llama_jni.cpp
)

target_include_directories(mangal_llama PRIVATE
    llama.cpp/include
    llama.cpp/ggml/include
)

target_link_libraries(mangal_llama
    llama
    android
    log
)
