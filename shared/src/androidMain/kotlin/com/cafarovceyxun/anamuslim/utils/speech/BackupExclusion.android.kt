package com.cafarovceyxun.anamuslim.utils.speech

/** Android yedəyi yalnız `backup_rules.xml`-də sadalananları götürür — model onsuz da kənardadır. */
internal actual fun excludeFromBackup(path: String) = Unit
