package com.ridi.oauth2

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.webkit.CookieManager
import com.ridi.books.helper.io.saveToFile
import com.ridi.oauth2.TokenManager.Companion.COOKIE_KEY_RIDI_AT
import com.ridi.oauth2.TokenManager.Companion.COOKIE_KEY_RIDI_RT
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File
import java.net.HttpURLConnection

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        private const val RIDI_AT_EXPIRES_AT_ZERO = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbmRyb2l" +
            "kS2ltIiwidV9pZHgiOjI2Mjc5MjUsImV4cCI6MCwiY2xpZW50X2lkIjoiTmt0MlhkYzB6TXVXbXllNk1Ta1lncUNoOXE2Smp" +
            "lTUNzVWlIMWtnTCIsInNjb3BlIjoiYWxsIn0.YVxEdViJVf450hLgrscUSFfmO-x-sl9MXaODMz5afbU"
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
        CookieManager.getInstance().removeAllCookies(null)
    }

    private val dispatcher: Dispatcher = object : Dispatcher() {

        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
            return if (request.requestUrl.toString().contains("ridi/authorize")) {
                if (request.headers.values("Cookie")[0] == "PHPSESSID=$INVALID_SESSION_ID;") {
                    MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                } else {
                    val atCookie = "$COOKIE_KEY_RIDI_AT=$RIDI_AT;"
                    val rtCookie = "$COOKIE_KEY_RIDI_RT=$RIDI_RT;"
                    MockResponse().setHeader("Location", APP_AUTHORIZED)
                        .setHeader("Set-Cookie", atCookie)
                        .addHeader("Set-Cookie", rtCookie)
                        .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
                }
            } else {
                val atCookie = "$COOKIE_KEY_RIDI_AT=$RIDI_AT_EXPIRES_AT_ZERO;"
                val rtCookie = "$COOKIE_KEY_RIDI_RT=$RIDI_RT;"
                MockResponse().setHeader("Set-Cookie", atCookie)
                    .addHeader("Set-Cookie", rtCookie)
                    .setResponseCode(HttpURLConnection.HTTP_OK)
            }
        }
    }

    @Test
    fun needClientId() {
        tokenManager.clientId = null
        tokenManager.sessionId = VALID_SESSION_ID
        tokenManager.tokenFile = tokenFile
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: IllegalStateException) {
            assertEquals(e::class, IllegalStateException::class)
            return
        }
        fail()
    }

    @Test
    fun needTokenFile() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.tokenFile = null
        tokenManager.sessionId = VALID_SESSION_ID
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: IllegalStateException) {
            assertEquals(e::class, IllegalStateException::class)
            return
        }
        fail()
    }

    @Test
    fun returnLoginURL() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.tokenFile = tokenFile
        tokenManager.sessionId = INVALID_SESSION_ID
        try {
            tokenManager.getAccessToken(APP_AUTHORIZED).blockingSingle()
        } catch (e: ResponseCodeException) {
            assertEquals(e::class, ResponseCodeException::class)
            assertEquals(e.message, "${HttpURLConnection.HTTP_OK}")
            return
        }
        fail()
    }

    @Test
    fun workProperly() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.sessionId = VALID_SESSION_ID
        tokenManager.tokenFile = tokenFile
        tokenManager.getAccessToken(APP_AUTHORIZED).blockingForEach {
            assertEquals(it.subject, "AndroidKim")
        }
    }

    @Test
    fun refreshToken() {
        tokenManager.clientId = CLIENT_ID
        tokenManager.sessionId = VALID_SESSION_ID

        val testJSON = JSONObject()
        testJSON.put(COOKIE_KEY_RIDI_AT, RIDI_AT)
        testJSON.put(COOKIE_KEY_RIDI_RT, RIDI_RT)
        testJSON.toString().saveToFile(tokenFile!!)
        tokenManager.tokenFile = tokenFile

        tokenManager.getAccessToken(APP_AUTHORIZED).blockingForEach {
            assertEquals(it.expiresAt, 0)
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
