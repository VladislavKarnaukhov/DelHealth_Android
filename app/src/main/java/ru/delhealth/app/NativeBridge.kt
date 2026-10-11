package ru.delhealth.app

import android.webkit.JavascriptInterface
/**
 * Мост для страниц сайта: `window.DelHealthNative`. Вызывается из JavaScript, поэтому методы работают
 * не в главном потоке — реальную работу делаем в [MainActivity]. Результат возвращается асинхронно в
 * `window.onDelHealthNative(event, payload)`.
 *
 * События: "status" {available, granted, state}, "permission" {granted}, "summary" {…} или "error" {message}.
 */
class NativeBridge(private val host: Host) {

    interface Host {
        fun healthStatus()
        fun requestHealthAccess()
        fun loadHealthSummary(days: Int)
        fun openHealthConnectInstall()
    }

    @JavascriptInterface fun isNativeApp(): Boolean = true

    @JavascriptInterface fun getHealthStatus() = host.healthStatus()

    @JavascriptInterface fun requestHealthAccess() = host.requestHealthAccess()

    @JavascriptInterface fun getHealthSummary(days: Int) = host.loadHealthSummary(days.coerceIn(1, 30))

    @JavascriptInterface fun installHealthConnect() = host.openHealthConnectInstall()
}
