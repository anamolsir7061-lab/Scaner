package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.ScanItem
import com.example.data.repository.ScanRepository
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.AnamolScannerTheme
import com.example.ui.viewmodel.ScanHistoryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanHistoryListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: ScanRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val key = ByteArray(32) { (it + 1).toByte() }
        val crypto = com.example.security.CryptoEngine.createWithRawKey(key)
        repository = ScanRepository(db.scanItemDao(), crypto)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `room dao returns encrypted qr and barcode items decrypted and sorted by timestamp descending`() = runBlocking {
        repository.recordScan(
            rawContent = "4006381333931",
            format = "EAN-13",
            parsedType = "text/plain",
            securityRating = "SAFE",
            title = "Oldest Barcode",
            timestamp = 1_000L
        )
        repository.recordScan(
            rawContent = "https://example.com/latest",
            format = "QR Code",
            parsedType = "text/uri-list",
            securityRating = "SAFE",
            title = "Newest QR",
            timestamp = 3_000L
        )
        repository.recordScan(
            rawContent = "ABC-12345",
            format = "Code 128",
            parsedType = "text/plain",
            securityRating = "SAFE",
            title = "Middle Barcode",
            timestamp = 2_000L
        )

        // Verify raw SQLite row stores AES-256-GCM ciphertext, not plaintext
        val rawRows = db.scanItemDao().getAll().first()
        assertTrue(rawRows.all { it.rawContent.startsWith("AESGCM:") })

        val ordered = repository.allScans.first()
        assertEquals(listOf("Newest QR", "Middle Barcode", "Oldest Barcode"), ordered.map { it.title })
        assertEquals(listOf(3_000L, 2_000L, 1_000L), ordered.map { it.timestamp })
        assertEquals("https://example.com/latest", ordered.first().rawContent)

        val searched = repository.searchScans("Barcode").first()
        assertEquals(listOf("Middle Barcode", "Oldest Barcode"), searched.map { it.title })
    }

    @Test
    fun `historyScreen displays room items in descending timestamp order`() {
        runBlocking {
            val dao = db.scanItemDao()
            dao.insert(
                ScanItem(
                    rawContent = "OLD-CODE-01",
                    format = "Code 39",
                    parsedType = "text/plain",
                    securityRating = "SAFE",
                    timestamp = 1_000L,
                    title = "First Scanned Item"
                )
            )
            dao.insert(
                ScanItem(
                    rawContent = "https://anamol.dev",
                    format = "QR Code",
                    parsedType = "text/uri-list",
                    securityRating = "SAFE",
                    timestamp = 5_000L,
                    title = "Latest Scanned QR"
                )
            )
        }

        val viewModel = ScanHistoryViewModel(repository)
        composeTestRule.setContent {
            AnamolScannerTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    onInspectItem = { _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scan_history_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Latest Scanned QR").assertIsDisplayed()
        composeTestRule.onNodeWithText("First Scanned Item").assertIsDisplayed()
    }
}
