package com.ridi.oauth2

import android.Manifest
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.rule.GrantPermissionRule
import com.ridi.books.helper.io.loadObject
import com.ridi.books.helper.io.saveToFile
import junit.framework.Assert.assertEquals
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.security.InvalidParameterException

class RidiOAuth2Test {
    @Rule
    @JvmField
    var mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context

    companion object {
        private const val VALID_SESSION_ID = "1"
        private const val INVALID_SESSION_ID = "2"
        private const val CLIENT_ID = "3"
        private const val APP_AUTHORIZED = "app://authorized"
        private const val RIDI_AT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBbmRyb2lkS2ltIiwi" +
            "bmFtZSI6IkFuZHJvaWRLaW0iLCJpYXQiOjE1MTYyMzkwMjJ9.Do4pxOtVe7zUQTG2MTHPVSEnb3kXX_Fb-uTfv_FOgUI"
        private const val RIDI_RT = "NHiVQz0ECBzlyI1asqsK6pfp32zvLD"
    }

    private var tokenFilePath = ""

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        RidiOAuth2.BASE_URL = mockWebServer.url("/").toString()

        mockWebServer.setDispatcher(dispatcher)

        context = InstrumentationRegistry.getContext()
        tokenFilePath = context.filesDir.absolutePath + "/tokenTest.json"

        if (File(tokenFilePath).exists()) {
            File(tokenFilePath).delete()
        }
    }

    @Test
    fun saveFile() {
        RidiOAuth2.instance.setTokenFilePath(tokenFilePath)
        val file = RidiOAuth2.tokenFile
        "AAA".saveToFile(RidiOAuth2.tokenFile)
        assertEquals(file.loadObject<String>(), "AAA")
    }

    private val dispatcher: Dispatcher = object : Dispatcher() {

        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
            return if (request.headers.values("Cookie")[0] == "PHPSESSID=$INVALID_SESSION_ID;") {
                MockResponse().setResponseCode(200)
            } else {
                val atCookies = "ridi-at=$RIDI_AT;"
                val rtCookies = "ridi-rt=$RIDI_RT;"
                MockResponse().setHeader("Location", APP_AUTHORIZED)
                    .setHeader("Set-Cookie", atCookies)
                    .addHeader("Set-Cookie", rtCookies)
                    .setResponseCode(302)
            }
        }
    }

    @Test
    fun needClientId() {
        RidiOAuth2.instance.setClientId("")
        RidiOAuth2.instance.setSessionId(VALID_SESSION_ID)
        RidiOAuth2.instance.setTokenFilePath(tokenFilePath)
        try {
            RidiOAuth2.instance.getOAuthToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: Exception) {
            assertEquals(e::class, IllegalStateException::class)
            return
        }
        fail()
    }

    @Test
    fun needTokenFilePath() {
        RidiOAuth2.instance.setClientId(CLIENT_ID)
        RidiOAuth2.instance.setSessionId(VALID_SESSION_ID)
        RidiOAuth2.instance.setTokenFilePath("")
        try {
            RidiOAuth2.instance.getOAuthToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: Exception) {
            assertEquals(e::class, RuntimeException::class)
            return
        }
        fail()
    }

    @Test
    fun returnLoginURL() {
        RidiOAuth2.instance.setClientId(CLIENT_ID)
        RidiOAuth2.instance.setSessionId(INVALID_SESSION_ID)
        RidiOAuth2.instance.setTokenFilePath(tokenFilePath)

        try {
            RidiOAuth2.instance.getOAuthToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: InvalidParameterException) {
            assertEquals(e::class, InvalidParameterException::class)
            assertEquals(e.message, "200")
            return
        }
        fail()
    }

    @Test
    fun workProperly() {
        RidiOAuth2.instance.setClientId(CLIENT_ID)
        RidiOAuth2.instance.setSessionId(VALID_SESSION_ID)
        RidiOAuth2.instance.setTokenFilePath(tokenFilePath)
        try {
            RidiOAuth2.instance.getOAuthToken(APP_AUTHORIZED).blockingForEach {
                assertEquals(it.subject, "AndroidKim")
            }
        } catch (e: Exception) {
            fail()
            return
        }
    }

    @Test
    fun checkCookieParsing() {
        RidiOAuth2.run {
            val jsonObject = JSONObject()
            jsonObject.parseCookie("ridi-rt=$RIDI_RT; Domain=; " +
                "expires=Sat, 21-Jul-2018 10:40:47 GMT; HttpOnly; Max-Age=2592000; Path=/; Secure")
            assertEquals(jsonObject.getString("ridi-rt"), RIDI_RT)
            jsonObject.parseCookie(
                "ridi-at=$RIDI_AT;Domain=; expires=Thu, 21-Jun-2018 11:40:47 GMT; HttpOnly; Max-Age=3600;" +
                    " Path=/; Secure")
            assertEquals(jsonObject.getString("ridi-at"), RIDI_AT)
        }
    }

    @After
    @Throws
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
