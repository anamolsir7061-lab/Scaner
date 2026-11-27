package com.example

import com.example.analyzer.HackerDataInspector
import com.example.analyzer.ParsedPayload
import com.example.analyzer.ThreatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HackerDataInspectorTest {

    @Test
    fun testUrlInspectionAndHttpsSafety() {
        val safeReport = HackerDataInspector.inspect("https://example.com/login?param=1")
        assertEquals(ThreatLevel.SAFE, safeReport.threatLevel)
        assertTrue(safeReport.payload is ParsedPayload.Url)
        val urlPayload = safeReport.payload as ParsedPayload.Url
        assertTrue(urlPayload.isHttps)
        assertEquals("example.com", urlPayload.host)

        val unencryptedReport = HackerDataInspector.inspect("http://phishing-site.xyz/login")
        assertEquals(ThreatLevel.DANGER, unencryptedReport.threatLevel)
    }

    @Test
    fun testWifiParsing() {
        val wifiReport = HackerDataInspector.inspect("WIFI:S:CyberLab;T:WPA;P:MatrixKey2026;H:false;;")
        assertTrue(wifiReport.payload is ParsedPayload.WiFi)
        val wifi = wifiReport.payload as ParsedPayload.WiFi
        assertEquals("CyberLab", wifi.ssid)
        assertEquals("MatrixKey2026", wifi.password)
        assertEquals("WPA", wifi.authType)
    }

    @Test
    fun testUpiParsing() {
        val upiReport = HackerDataInspector.inspect("upi://pay?pa=cyber@okhdfcbank&pn=CyberMerchant&am=250.00&cu=INR")
        assertTrue(upiReport.payload is ParsedPayload.Upi)
        val upi = upiReport.payload as ParsedPayload.Upi
        assertEquals("cyber@okhdfcbank", upi.vpa)
        assertEquals("CyberMerchant", upi.payeeName)
        assertEquals("250.00", upi.amount)
    }

    @Test
    fun testHexDumpGeneration() {
        val report = HackerDataInspector.inspect("HELLO WORLD")
        assertNotNull(report.hexDump)
        assertTrue(report.hexDump.contains("00000000:"))
        assertTrue(report.hexDump.contains("HELLO WORLD"))
    }

    @Test
    fun testShortenedUrlSafetyWarning() {
        val report = HackerDataInspector.inspect("https://bit.ly/3xYzAbc")
        assertEquals(ThreatLevel.WARNING, report.threatLevel)
        assertTrue(report.payload is ParsedPayload.Url)
        assertTrue((report.payload as ParsedPayload.Url).isShortenedUrl)
    }

    @Test
    fun testMecardContactParsing() {
        val report = HackerDataInspector.inspect("MECARD:N:Anamol;TEL:+911234567890;EMAIL:a@b.com;;")
        assertTrue(report.payload is ParsedPayload.VCard)
        val contact = report.payload as ParsedPayload.VCard
        assertEquals("Anamol", contact.formattedName)
        assertEquals(listOf("+911234567890"), contact.phoneNumbers)
        assertEquals(listOf("a@b.com"), contact.emails)
    }

    @Test
    fun testCoordinateMapperAndAutoZoom() {
        val mapped = com.example.scanner.QrCoordinateMapper.mapToNormalizedOverlay(
            rawValue = "TEST",
            formatName = "QR Code",
            formatCode = 256,
            cornerPoints = null,
            boundingBox = null,
            imageWidth = 1280,
            imageHeight = 720,
            rotationDegrees = 90,
            viewWidth = 1080f,
            viewHeight = 2400f
        )
        assertTrue(mapped.centerX in 0f..1f)
        assertTrue(mapped.centerY in 0f..1f)

        val zoom = com.example.scanner.QrCoordinateMapper.computeSuggestedZoom(
            currentZoom = 1.0f,
            maxZoom = 4.0f,
            barcodeWidthRatio = 0.08f,
            barcodeHeightRatio = 0.08f
        )
        assertNotNull(zoom)
        assertTrue(zoom!! > 1.0f)
    }
}
