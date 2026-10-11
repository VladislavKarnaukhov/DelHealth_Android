package ru.delhealth.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.delhealth.app.health.HealthConnectManager
import ru.delhealth.app.health.HealthConnectManager.Availability

class MainActivity : AppCompatActivity(), NativeBridge.Host {

    private lateinit var webView: WebView
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var health: HealthConnectManager

    private val siteUri = Uri.parse(BuildConfig.SITE_URL)

    private val permissionLauncher =
        registerForActivityResult(HealthConnectManager.permissionContract()) { granted ->
            emit("permission", JSONObject().put("granted", granted.containsAll(HealthConnectManager.PERMISSIONS)))
        }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_DelHealth)
        setContentView(R.layout.activity_main)

        health = HealthConnectManager(this)
        webView = findViewById(R.id.webview)
        refresh = findViewById(R.id.refresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            userAgentString = "$userAgentString DelHealthApp/${BuildConfig.VERSION_NAME}"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        webView.addJavascriptInterface(NativeBridge(this), "DelHealthNative")
        webView.webViewClient = SiteClient()

        refresh.setOnRefreshListener { webView.reload() }
        refresh.setColorSchemeResources(R.color.accent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })

        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else webView.loadUrl(intent?.data?.takeIf { isSite(it) }?.toString() ?: BuildConfig.SITE_URL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("DelHealthNative")
        webView.destroy()
        super.onDestroy()
    }

    private fun isSite(uri: Uri) = uri.scheme == "https" && uri.host == siteUri.host

    // ---- NativeBridge.Host ---------------------------------------------------------------------------------

    override fun healthStatus() {
        runOnUiThread {
            lifecycleScope.launch {
                runCatching {
                    val state = health.availability
                    val granted = state == Availability.AVAILABLE && health.hasAllPermissions()
                    emit("status", JSONObject().put("state", state.name).put("available", state == Availability.AVAILABLE).put("granted", granted))
                }.onFailure { emitError(it) }
            }
        }
    }

    override fun requestHealthAccess() {
        runOnUiThread {
            if (health.availability != Availability.AVAILABLE) healthStatus()
            else permissionLauncher.launch(HealthConnectManager.PERMISSIONS)
        }
    }

    override fun loadHealthSummary(days: Int) {
        runOnUiThread {
            lifecycleScope.launch {
                runCatching {
                    if (health.availability != Availability.AVAILABLE || !health.hasAllPermissions()) error("no_access")
                    emit("summary", health.readSummary(days))
                }.onFailure { emitError(it) }
            }
        }
    }

    override fun openHealthConnectInstall() {
        runOnUiThread {
            val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
            }
        }
    }

    /** Отправляет событие странице: window.onDelHealthNative(event, payload). Только для страниц нашего сайта. */
    private fun emit(event: String, payload: JSONObject) {
        val url = webView.url?.let(Uri::parse)
        if (url == null || !isSite(url)) return
        val js = "window.onDelHealthNative && window.onDelHealthNative(${JSONObject.quote(event)}, $payload);"
        webView.evaluateJavascript(js, null)
    }

    private fun emitError(t: Throwable) = emit("error", JSONObject().put("message", t.message ?: t.javaClass.simpleName))

    // ---- WebView -------------------------------------------------------------------------------------------

    private inner class SiteClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (isSite(uri)) return false
            // Чужие адреса, tel:, mailto: открываем снаружи — сайт не должен получать доступ к мосту.
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: ActivityNotFoundException) {}
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { refresh.isRefreshing = true }

        override fun onPageFinished(view: WebView, url: String?) { refresh.isRefreshing = false }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                refresh.isRefreshing = false
                view.loadUrl("file:///android_asset/offline.html")
            }
        }
    }
}
