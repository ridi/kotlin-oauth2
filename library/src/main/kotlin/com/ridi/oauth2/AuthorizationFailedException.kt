package com.ridi.oauth2

class AuthorizationFailedException(
    val httpStatusCode: Int,
    val errorCode: String? = null,
    errorDescription: String? = "status=$httpStatusCode errorCode=$errorCode"
) : RuntimeException(errorDescription)
