/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 2/2/2023.
 * All rights reserved.
 */

package com.cafarovceyxun.anamuslim.api

object ApiConfig {
    const val GH_PROXY_ROOT = "https://gh-proxy.alfaazplus.com/"
    const val JS_DELIVR_ROOT = "https://cdn.jsdelivr.net/gh/"
    const val GH_RAW_ROOT = "https://raw.githubusercontent.com/"
    const val GH_PROXY_BASE_URL = "${GH_PROXY_ROOT}AlfaazPlus/QuranApp/master/"
    const val JS_DELIVR_BASE_URL = "${JS_DELIVR_ROOT}AlfaazPlus/QuranApp@latest/"
    const val GH_RAW_BASE_URL = "${GH_RAW_ROOT}AlfaazPlus/QuranApp/master/"
    const val GITHUB_REPOSITORY_URL = "https://github.com/cafarovceyxun/AnaMuslim"

    // KFQPC page-font archives (`.tar.gz`) served from this project's own GitHub
    // Releases (tag `qpc`) instead of the upstream AlfaazPlus inventory, so the app
    // no longer depends on third-party infrastructure for fonts. Create a release
    // tagged `qpc` on the repo above and upload the archives named in
    // `ScriptFontInstaller.archiveNameFor`.
    const val QPC_FONT_RELEASE_BASE_URL = "$GITHUB_REPOSITORY_URL/releases/download/qpc/"

    const val GITHUB_ISSUES_URL = "https://github.com/cafarovceyxun/AnaMuslim/issues"
    const val GITHUB_PRIVACY_POLICY_URL = "https://github.com/cafarovceyxun/AnaMuslim/blob/main/PRIVACY.md"
    // `?template=` must name a file that exists in `.github/ISSUE_TEMPLATE/`, extension included —
    // otherwise GitHub silently drops the preselection and shows the template chooser.
    const val GITHUB_ISSUES_BUG_REPORT_URL =
        "https://github.com/cafarovceyxun/AnaMuslim/issues/new?template=bug_report.md"

    const val QURAN_API_ROOT_URL = "https://api.quran.com/"
    const val ALFAAZPLUS_API_ROOT_URL = "https://api.alfaazplus.com"
}
