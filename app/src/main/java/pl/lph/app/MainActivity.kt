package pl.lph.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        enableWebViewDebugging()
        configureWebView()
        configureBackNavigation()

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL)
        } else {
            val restored = webView.restoreState(savedInstanceState)

            if (restored == null) {
                webView.loadUrl(HOME_URL)
            }
        }
    }

    private fun enableWebViewDebugging() {

        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView() {

        webView.settings.apply {

            javaScriptEnabled = true
            domStorageEnabled = true

            loadsImagesAutomatically = true

            cacheMode = WebSettings.LOAD_DEFAULT

            // Zewnętrzne zasoby strony.
            mixedContentMode =
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Nie przedstawiamy aplikacji jako typowy Android WebView.
            userAgentString = userAgentString
                .replace("; wv", "")
                .replace("Version/4.0 ", "")

            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)

            javaScriptCanOpenWindowsAutomatically = true
        }

        /*
         * Blogger / Apps Script / osadzone widgety mogą korzystać
         * z cookies pochodzących z innej domeny.
         */
        CookieManager.getInstance().apply {

            setAcceptCookie(true)

            setAcceptThirdPartyCookies(
                webView,
                true
            )
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                if (!request.isForMainFrame) {
                    return false
                }

                val uri = request.url

                if (isLphUrl(uri)) {
                    return false
                }

                openExternalLink(uri)

                return true
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                Log.d(
                    LOG_TAG,
                    "Załadowano: $url"
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(
                    view,
                    request,
                    error
                )

                Log.e(
                    LOG_TAG,
                    "WEB ERROR: ${request?.url} | " +
                            "code=${error?.errorCode} | " +
                            "description=${error?.description}"
                )
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(
                    view,
                    request,
                    errorResponse
                )

                Log.e(
                    LOG_TAG,
                    "HTTP ERROR: ${request?.url} | " +
                            "status=${errorResponse?.statusCode} | " +
                            "reason=${errorResponse?.reasonPhrase}"
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(
                view: WebView?,
                newProgress: Int
            ) {

                progressBar.progress = newProgress

                progressBar.visibility =
                    if (newProgress >= 100) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
            }

            /*
             * Komunikaty JavaScript będą teraz widoczne
             * w Logcat Android Studio.
             */
            override fun onConsoleMessage(
                consoleMessage: ConsoleMessage
            ): Boolean {

                Log.d(
                    LOG_TAG,
                    "JS: ${consoleMessage.message()} " +
                            "[${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}]"
                )

                return true
            }
        }
    }

    private fun configureBackNavigation() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (webView.canGoBack()) {

                        webView.goBack()

                    } else {

                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun isLphUrl(uri: Uri): Boolean {

        val host = uri.host?.lowercase()
            ?: return false

        return uri.scheme in setOf("http", "https") &&
                (
                        host == LPH_HOST ||
                                host == "www.$LPH_HOST"
                        )
    }

    private fun openExternalLink(uri: Uri) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
            )

        } catch (_: ActivityNotFoundException) {

            Log.w(
                LOG_TAG,
                "Brak aplikacji obsługującej: $uri"
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        webView.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        webView.stopLoading()

        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()

        webView.destroy()

        super.onDestroy()
    }

    companion object {

        private const val LPH_HOST =
            "ligapoznegohamowania.blogspot.com"

        private const val HOME_URL =
            "https://$LPH_HOST/"

        private const val LOG_TAG =
            "LPH-WebView"
    }
}