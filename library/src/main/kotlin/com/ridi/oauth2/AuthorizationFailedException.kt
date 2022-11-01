package com.ridi.oauth2

class AuthorizationFailedException(
    val httpStatusCode: Int,
    val errorCode: String? = null,
    val errorDescription: String? = null
) : RuntimeException(errorDescription ?: "status=$httpStatusCode errorCode=$errorCode")
