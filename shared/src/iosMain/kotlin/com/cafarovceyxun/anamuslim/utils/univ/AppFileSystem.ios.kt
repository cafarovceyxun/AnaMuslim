package com.cafarovceyxun.anamuslim.utils.univ

import okio.FileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun appFilesDirPath(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String

internal actual fun platformFileSystem(): FileSystem = FileSystem.SYSTEM
