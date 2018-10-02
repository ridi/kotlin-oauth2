package com.ridi.oauth2

import com.auth0.jwt.JWT
import org.junit.Assert
import org.junit.Test

class AuthorizationTest {
    private val clientId = System.getenv("TEST_CLIENT_ID")
    private val clientSecret = System.getenv("TEST_CLIENT_SECRET")
    private val userIdx = System.getenv("TEST_USER_IDX").toInt()
    private val username = System.getenv("TEST_USERNAME")
    private val password = System.getenv("TEST_PASSWORD")

    @Test
    fun `test password grant authorization and token refresh`() {
        val authorization = Authorization(clientId, clientSecret, true)

        fun TokenResponse.assert() {
            val jwt = JWT.decode(accessToken)
            Assert.assertEquals(userIdx, jwt.getClaim("u_idx").asInt())
            Assert.assertEquals(username, jwt.subject)

            Assert.assertEquals("Bearer", tokenType)
            Assert.assertEquals("all", scope)
        }

        authorization.requestPasswordGrantAuthorization(username, password).blockingGet().run {
            assert()
            authorization.refreshAccessToken(refreshToken).blockingGet().assert()
        }
    }
}
