package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable

// The Compose Multiplatform navigation host is wired now; back routes through the pop action the
// hosts publish via LocalSystemBack (chained for the nested settings graph).
@Composable
actual fun rememberSystemBack(): (() -> Unit)? = LocalSystemBack.current
