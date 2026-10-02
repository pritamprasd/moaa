package dev.pritam.host

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import dev.pritam.ghostagent.GhostAgentManager
import dev.pritam.host.host.HostApplicationContainer
import dev.pritam.host.tool.nfc.manager.NfcManager

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val container by lazy { HostApplicationContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Initialize Ghost Agent repository and notification channel
        GhostAgentManager.init(applicationContext)

        handleNfcIntent(intent)
        handleShortcutIntent(intent)
        handleDeepLinkIntent(intent)

        setContent {
            HostApp(container = container)
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        disableNfcReaderMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
        handleShortcutIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val targetToolId = intent?.getStringExtra(dev.pritam.host.shortcut.ShortcutUtils.EXTRA_TARGET_TOOL_ID)
        if (!targetToolId.isNullOrBlank()) {
            container.state.setPendingTargetToolId(targetToolId)
        }
    }

    /** Handles deep-links from Ghost Agent floating bubble and Quick Settings tile. */
    private fun handleDeepLinkIntent(intent: Intent?) {
        val deepLinkToolId = intent?.getStringExtra("deep_link_tool")
        if (!deepLinkToolId.isNullOrBlank()) {
            container.state.setPendingTargetToolId(deepLinkToolId)
        }
    }

    private fun enableNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        adapter.enableReaderMode(this, { tag ->
            runOnUiThread {
                NfcManager.notifyTagDiscovered(tag)
            }
        }, flags, null)
    }

    private fun disableNfcReaderMode() {
        nfcAdapter?.disableReaderMode(this)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let { NfcManager.notifyTagDiscovered(it) }
    }
}