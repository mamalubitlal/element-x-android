# /*
#  * Copyright (c) 2025 Element Creations Ltd.
#  *
#  * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
#  * Please see LICENSE files in the repository root for full details.
#  */

cmake_minimum_required(VERSION 3.22.1)

project(dpi_bypass_native C CXX)

# Set C++ standard globally
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ============== ByeDPI ==============
file(GLOB BYE_DPI_SRC "byedpi/*.c")
list(REMOVE_ITEM BYE_DPI_SRC "${CMAKE_CURRENT_SOURCE_DIR}/byedpi/win_service.c")

add_library(byedpi SHARED ${BYE_DPI_SRC} native-lib.c)
target_include_directories(byedpi PRIVATE byedpi)
set_source_files_properties(${BYE_DPI_SRC} native-lib.c PROPERTIES C_STANDARD 99 C_STANDARD_REQUIRED ON)

# Apply C++17 standard only to C++ files via generator expression
target_compile_options(byedpi PRIVATE
    $<$<COMPILE_LANGUAGE:CXX>:-std=c++17>
    -O2 -Wall -Wno-unused -Wextra -Wno-unused-parameter -pedantic
)
target_compile_definitions(byedpi PRIVATE ANDROID_APP)

target_link_libraries(byedpi PRIVATE android log)

# Note: hev-socks5-tunnel is included as a submodule for potential future use
# but not built by default (app-only solution uses direct proxy)
