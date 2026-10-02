package dev.motherofallapps.host

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import dev.motherofallapps.host.host.HostApplicationContainer
import dev.motherofallapps.host.tool.nfc.manager.NfcManager

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val container = HostApplicationContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        handleNfcIntent(intent)
        handleShortcutIntent(intent)

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
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val targetToolId = intent?.getStringExtra(dev.motherofallapps.host.shortcut.ShortcutUtils.EXTRA_TARGET_TOOL_ID)
        if (!targetToolId.isNullOrBlank()) {
            container.state.setPendingTargetToolId(targetToolId)
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