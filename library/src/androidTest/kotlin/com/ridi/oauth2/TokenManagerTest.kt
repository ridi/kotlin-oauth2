package com.ridi.oauth2

import android.support.test.InstrumentationRegistry
import com.ridi.books.helper.io.saveToFile
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.util.Date

class TokenManagerTest {
    companion object {
        private const val VALID_SESSION_ID = "1"
        private const val INVALID_SESSION_ID = "2"
        private const val CLIENT_ID = "3"
        private const val APP_AUTHORIZED = "app_authorized"
        private const val LOGIN_PAGE = "login?return_url=login_required"
        private const val RIDI_AT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbmRyb2lkS2ltIiwidV9pZHg" +
            "iOjI2Mjc5MjUsImV4cCI6MTUzMDc2MTcwNywiY2xpZW50X2lkIjoiTmt0MlhkYzB6TXVXbXllNk1Ta1lncUNoOXE2SmplTUN" +
            "zVWlIMWtnTCIsInNjb3BlIjoiYWxsIn0.KP_jrSc1KZ36-TYf-oiTyMl2Zn-dm9C8x-eY0bV0uQ8"
        private const val RIDI_AT_EXPIRES_AT_ZERO = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbmRyb2l" +
            "kS2ltIiwidV9pZHgiOjI2Mjc5MjUsImV4cCI6MCwiY2xpZW50X2lkIjoiTmt0MlhkYzB6TXVXbXllNk1Ta1lncUNoOXE2Smp" +
            "lTUNzVWlIMWtnTCIsInNjb3BlIjoiYWxsIn0.YVxEdViJVf450hLgrscUSFfmO-x-sl9MXaODMz5afbU"
        private const val RIDI_RT = "NHiVQz0ECBzlyI1asqsK6pfp32zvLD"
    }

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var tokenFile: File

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            start()
            setDispatcher(object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val url = request.requestUrl.toString()
                    val atCookie = "${TokenManager.COOKIE_NAME_RIDI_AT}=$RIDI_AT_EXPIRES_AT_ZERO;"
                    val rtCookie = "${TokenManager.COOKIE_NAME_RIDI_RT}=$RIDI_RT;"

                    return if (url.contains("ridi/authorize")) {
                        MockResponse().setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP).run {
                            if (request.headers.values("Cookie").contains("PHPSESSID=$INVALID_SESSION_ID")) {
                                setHeader("Location", LOGIN_PAGE)
                            } else {
                                setHeader("Location", APP_AUTHORIZED)
                                addHeader("Set-Cookie", atCookie)
                                addHeader("Set-Cookie", rtCookie)
                            }
                        }
                    } else if (url.contains(LOGIN_PAGE) || url.contains(APP_AUTHORIZED)) {
                        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                    } else if (url.contains("ridi/token")) {
                        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                            .addHeader("Set-Cookie", atCookie)
                            .addHeader("Set-Cookie", rtCookie)
                    } else {
                        MockResponse().setResponseCode(HttpURLConnection.HTTP_FORBIDDEN)
                    }
                }
            })
        }

        tokenManager = TokenManager().apply {
            baseUrl = mockWebServer.url("/").toString()
        }

        tokenFile = File(InstrumentationRegistry.getContext().filesDir.absolutePath, "tokenTest.json").apply {
            if (exists()) {
                delete()
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
        } catch (e: UnexpectedResponseException) {
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

        JSONObject()
            .put(TokenManager.COOKIE_NAME_RIDI_AT, RIDI_AT)
            .put(TokenManager.COOKIE_NAME_RIDI_RT, RIDI_RT)
            .toString().saveToFile(tokenFile!!)
        tokenManager.tokenFile = tokenFile

        tokenManager.getAccessToken(APP_AUTHORIZED).blockingForEach {
            assertEquals(it.expiresAt, Date(0))
        }
    }

    @Test
    fun checkCookieParsing() {
        TokenManager.run {
            val jsonObject = JSONObject()
            jsonObject.addTokensFromCookie(Cookie.parse(HttpUrl.parse(tokenManager.baseUrl)!!,
                "$COOKIE_NAME_RIDI_RT=$RIDI_RT; Domain=; expires=Sat, 21-Jul-2018 10:40:47 GMT; HttpOnly; " +
                    "Max-Age=2592000; Path=/; Secure")!!)
            assertEquals(jsonObject.getString(COOKIE_NAME_RIDI_RT), RIDI_RT)
            jsonObject.addTokensFromCookie(Cookie.parse(HttpUrl.parse(tokenManager.baseUrl)!!,
                "$COOKIE_NAME_RIDI_AT=$RIDI_AT;Domain=; expires=Thu, 21-Jun-2018 11:40:47 GMT; HttpOnly; " +
                    "Max-Age=3600; Path=/; Settings.Secure")!!)
            assertEquals(jsonObject.getString(COOKIE_NAME_RIDI_AT), RIDI_AT)
        }
    }

    @After
    @Throws
    fun tearDown() {
        mockWebServer.shutdown()
    }
}