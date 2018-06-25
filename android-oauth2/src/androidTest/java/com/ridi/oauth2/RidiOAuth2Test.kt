package com.ridi.oauth2

import android.Manifest
import android.os.Environment
import android.support.test.rule.GrantPermissionRule
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.security.InvalidParameterException

class RidiOAuth2Test {
    @Rule
    @JvmField
    var mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val redirectUri = "app://authorized"

    @Test
    fun needClientId() {
        RidiOAuth2.instance.setDev()
        RidiOAuth2.instance.setTokenFilePath(Environment.getExternalStorageDirectory().absolutePath + "/tokenJSON.json")
        if (RidiOAuth2.tokenFile.exists()) {
            RidiOAuth2.tokenFile.delete()
        }
        RidiOAuth2.instance.setClientId("")
        try {
            RidiOAuth2.instance.getOAuthToken(redirectUri).blockingSingle()
        } catch (e: IllegalStateException) {
            assertEquals(e::class, IllegalStateException::class)
            return
        }
        fail()
    }

    @Test
    fun returnLoginURL() {
        RidiOAuth2.instance.setDev()
        RidiOAuth2.instance.setTokenFilePath(Environment.getExternalStorageDirectory().absolutePath + "/tokenJSON.json")
        if (RidiOAuth2.tokenFile.exists()) {
            RidiOAuth2.tokenFile.delete()
        }
        RidiOAuth2.instance.setClientId("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL")
        RidiOAuth2.instance.setSessionId("")
        try {
            RidiOAuth2.instance.getOAuthToken(redirectUri).blockingSingle()
        } catch (e: InvalidParameterException) {
            assertEquals(e::class, InvalidParameterException::class)
            assertEquals(e.message, "200")
            return
        }
        fail()
    }

    @Test
    fun checkCookieParsing() {
        RidiOAuth2.run {
            val jsonObject = JSONObject()
            jsonObject.parseCookie("ridi-rt=NHiVQz0ECBzlyI1hsqsK6pfp32zvLD; Domain=dev.ridi.io; " +
                "expires=Sat, 21-Jul-2018 10:40:47 GMT; HttpOnly; Max-Age=2592000; Path=/; Secure")
            assertEquals(jsonObject.getString("ridi-rt"), "NHiVQz0ECBzlyI1hsqsK6pfp32zvLD")
            jsonObject.parseCookie(
                "ridi-at=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJodW42NzI4IiwidV9pZ" +
                    "HgiOjI2MjU4MTcsImV4cCI6MTUyOTU4MTI0OCwiY2xpZW50X2lkIjoiTmt0MlhkYzB6TXVXbXllNk1Ta1lncUNo" +
                    "OXE2SmplTUNzVWlIMWtnTCIsInNjb3BlIjoiYWxsIn0.52p_iA3vrTwdWglfAnSbbFd1fIMZLeO4aTV8raVK93k;" +
                    " Domain=dev.ridi.io; expires=Thu, 21-Jun-2018 11:40:47 GMT; HttpOnly; Max-Age=3600;" +
                    " Path=/; Secure")
            assertEquals(jsonObject.getString("ridi-at"), "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
                "eyJzdWIiOiJodW42NzI4IiwidV9pZHgiOjI2MjU4MTcsImV4cCI6MTUyOTU4MTI0OCwiY2xpZW50X2lkIjoiTmt0MlhkYz" +
                "B6TXVXbXllNk1Ta1lncUNoOXE2SmplTUNzVWlIMWtnTCIsInNjb3BlIjoiYWxsIn0.52p_iA3vrTwdWglfAnSbbFd1fIMZL" +
                "eO4aTV8raVK93k")
        }
    }
}
