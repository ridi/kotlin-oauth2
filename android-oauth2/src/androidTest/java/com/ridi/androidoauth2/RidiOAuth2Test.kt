package com.ridi.androidoauth2

import android.Manifest
import android.support.test.rule.GrantPermissionRule
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.junit.Rule
import org.junit.Test

class RidiOAuth2Test {
    @Rule
    @JvmField
    var mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    fun needClientId() {
        RidiOAuth2.setClientId("")
        RidiOAuth2.getOAuthToken("app://authorized").subscribe({
            fail()
        }, {
            assertEquals(it.message, "Client Id not initialized")
        })
    }

    @Test
    fun loginURLReturn() {
        RidiOAuth2.setClientId("Nkt2Xdc0zMuWmye6MSkYgqCh9q6JjeMCsUiH1kgL")
        RidiOAuth2.cookies = HashSet()
        RidiOAuth2.getOAuthToken("app://authorized").subscribe({
            assertEquals(it, "Status code Error 200")
        }, {
            it.printStackTrace()
            fail()
        })
    }
}