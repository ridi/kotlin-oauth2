package com.ridi.oauth2

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Int,
    val tokenType: String,
    val scope: String,
    val refreshToken: String,
    val refreshTokenExpiresIn: Int
)
