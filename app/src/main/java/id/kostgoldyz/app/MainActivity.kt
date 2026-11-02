package id.kostgoldyz.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Kost Goldyz (versi Claude).
 * Membuka aplikasi Kost Goldyz di Claude memakai Chrome, jadi login Google,
 * data yang tersinkron, dan AI pembaca bukti transfer semuanya berjalan seperti di Chrome.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = Uri.parse(getString(R.string.app_url))

        val colors = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(0xFF0B4A3D.toInt())
            .setNavigationBarColor(0xFFFBFDFB.toInt())
            .build()

        val tab = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colors)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()

        // Utamakan Chrome supaya memakai login Claude yang sama.
        val browser = CustomTabsClient.getPackageName(this, listOf("com.android.chrome"), true)
            ?: CustomTabsClient.getPackageName(this, null)
        if (browser != null) tab.intent.setPackage(browser)

        try {
            tab.launchUrl(this, url)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(this, "Pasang Google Chrome untuk membuka Kost Goldyz", Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }
}
