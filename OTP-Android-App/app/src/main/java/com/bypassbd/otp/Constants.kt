package com.bypassbd.otp

object Constants {
    /**
     * Your worker's base URL — the SAME value in the extension's config.js
     * (self.VA_WORKER_URL). This is the ONLY place the URL is set; it is fixed in code
     * and never shown or editable in the app UI. Change it only if you move the worker.
     * No trailing slash, no "/v1".
     */
    const val DEFAULT_SERVER = "https://otp-verify.bypassbd.workers.dev"
}
