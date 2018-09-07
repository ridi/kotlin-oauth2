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
    private lateinit var apiBaseUrl: String
    private lateinit var tokenSaveFile: File
    private lateinit var tokenManager: TokenManager

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
                                setHeader("Location", TokenManager.AUTHORIZATION_REDIRECT_URI)
                                addHeader("Set-Cookie", atCookie)
                                addHeader("Set-Cookie", rtCookie)
                            }
                        }
                    } else if (url.contains(LOGIN_PAGE) || url.contains(TokenManager.AUTHORIZATION_REDIRECT_URI)) {
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
        apiBaseUrl = mockWebServer.url("/").toString()
        tokenSaveFile = File(InstrumentationRegistry.getContext().filesDir, "tokenTest.json").apply {
            if (exists()) {
                delete()
            }
        }
        tokenManager = TokenManager(apiBaseUrl, CLIENT_ID, tokenSaveFile)
    }

    @Test
    fun testAcquiringAccessToken() {
        tokenManager.getAccessToken().blockingForEach {
            assertEquals(it.subject, "AndroidKim")
        }
    }

    @Test
    fun testTokenRefresh() {
        tokenManager.phpSessionId = VALID_SESSION_ID

        JSONObject()
            .put(TokenManager.COOKIE_NAME_RIDI_AT, RIDI_AT)
            .put(TokenManager.COOKIE_NAME_RIDI_RT, RIDI_RT)
            .toString().saveToFile(tokenSaveFile)

        tokenManager.getAccessToken().blockingForEach {
            assertEquals(it.expiresAt, Date(0))
        }
    }

    @Test
    fun testCookieParsing() {
        TokenManager.run {
            val jsonObject = JSONObject()
            jsonObject.addTokensFromCookie(Cookie.parse(HttpUrl.parse(apiBaseUrl)!!,
                "$COOKIE_NAME_RIDI_RT=$RIDI_RT; Domain=; expires=Sat, 21-Jul-2018 10:40:47 GMT; HttpOnly; " +
                    "Max-Age=2592000; Path=/; Secure")!!)
            assertEquals(jsonObject.getString(COOKIE_NAME_RIDI_RT), RIDI_RT)
            jsonObject.addTokensFromCookie(Cookie.parse(HttpUrl.parse(apiBaseUrl)!!,
                "$COOKIE_NAME_RIDI_AT=$RIDI_AT;Domain=; expires=Thu, 21-Jun-2018 11:40:47 GMT; HttpOnly; " +
                    "Max-Age=3600; Path=/; Settings.Secure")!!)
            assertEquals(jsonObject.getString(COOKIE_NAME_RIDI_AT), RIDI_AT)
        }
    }

    @Test
    fun testRedirectingToLoginPage() {
        tokenManager.phpSessionId = INVALID_SESSION_ID
        try {
            tokenManager.getAccessToken().blockingSingle()
        } catch (e: UnexpectedResponseException) {
            return
        }
        fail()
    }

    @After
    @Throws
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
