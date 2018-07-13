package com.ridi.oauth2

import android.content.Context
import android.support.test.InstrumentationRegistry
import com.ridi.oauth2.TokenManager.Companion.COOKIE_KEY_RIDI_AT
import com.ridi.oauth2.TokenManager.Companion.COOKIE_KEY_RIDI_RT
import junit.framework.Assert.assertEquals
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.security.InvalidParameterException

class TokenManagerTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context

    companion object {
        private const val VALID_SESSION_ID = "1"
        private const val INVALID_SESSION_ID = "2"
        private const val CLIENT_ID = "3"
        private const val APP_AUTHORIZED = "app://authorized"
        private const val RIDI_AT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbmRyb2lkS2ltIiwidV9pZHg" +
            "iOjI2Mjc5MjUsImV4cCI6MTUzMDc2MTcwNywiY2xpZW50X2lkIjoiTmt0MlhkYzB6TXVXbXllNk1Ta1lncUNoOXE2SmplTUN" +
            "zVWlIMWtnTCIsInNjb3BlIjoiYWxsIn0.KP_jrSc1KZ36-TYf-oiTyMl2Zn-dm9C8x-eY0bV0uQ8"
        private const val RIDI_RT = "NHiVQz0ECBzlyI1asqsK6pfp32zvLD"
    }

    private var tokenFile: File? = null
    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        TokenManager.BASE_URL = mockWebServer.url("/").toString()
        mockWebServer.setDispatcher(dispatcher)

        context = InstrumentationRegistry.getContext()
        tokenFile = File(context.filesDir.absolutePath + "/tokenTest.json")
        if (tokenFile!!.exists()) {
            tokenFile!!.delete()
        }
        tokenManager = TokenManager()
    }

    private val dispatcher: Dispatcher = object : Dispatcher() {

        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
            return if (request.headers.values("Cookie")[0] == "PHPSESSID=$INVALID_SESSION_ID;") {
                MockResponse().setResponseCode(200)
            } else {
                val atCookies = "$COOKIE_KEY_RIDI_AT=$RIDI_AT;"
                val rtCookies = "$COOKIE_KEY_RIDI_RT=$RIDI_RT;"
                MockResponse().setHeader("Location", APP_AUTHORIZED)
                    .setHeader("Set-Cookie", atCookies)
                    .addHeader("Set-Cookie", rtCookies)
                    .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
            }
        }
    }

    @Test
    fun needClientId() {
        tokenManager.clientId = null
        tokenManager.setSessionId(VALID_SESSION_ID)
        tokenManager.tokenFile = tokenFile
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: Exception) {
            assertEquals(e::class, IllegalStateException::class)
            return
        }
        fail()
    }

    @Test
    fun needTokenFile() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.tokenFile = null
        tokenManager.setSessionId(VALID_SESSION_ID)
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: Exception) {
            assertEquals(e::class, IllegalStateException::class)
            return
        }
        fail()
    }

    @Test
    fun returnLoginURL() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.tokenFile = tokenFile
        tokenManager.setSessionId(INVALID_SESSION_ID)
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: InvalidParameterException) {
            assertEquals(e::class, InvalidParameterException::class)
            assertEquals(e.message, "${HttpURLConnection.HTTP_OK}")
            return
        }
        fail()
    }

    @Test
    fun workProperly() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.setSessionId(VALID_SESSION_ID)
        tokenManager.tokenFile = tokenFile
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingForEach {
                assertEquals(it.subject, "AndroidKim")
            }
        } catch (e: Exception) {
            fail()
            return
        }
    }

    @Test
    fun checkCookieParsing() {
        TokenManager.run {
            val jsonObject = JSONObject()
            jsonObject.parseCookie("$COOKIE_KEY_RIDI_RT=$RIDI_RT; Domain=; " +
                "expires=Sat, 21-Jul-2018 10:40:47 GMT; HttpOnly; Max-Age=2592000; Path=/; Secure")
            assertEquals(jsonObject.getString(COOKIE_KEY_RIDI_RT), RIDI_RT)
            jsonObject.parseCookie(
                "$COOKIE_KEY_RIDI_AT=$RIDI_AT;Domain=; expires=Thu, 21-Jun-2018 11:40:47 GMT; HttpOnly; " +
                    "Max-Age=3600; Path=/; Settings.Secure")
            assertEquals(jsonObject.getString(COOKIE_KEY_RIDI_AT), RIDI_AT)
        }
    }

    @After
    @Throws
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
