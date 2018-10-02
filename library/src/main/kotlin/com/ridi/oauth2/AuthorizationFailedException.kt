package com.ridi.oauth2

class AuthorizationFailedException(
    val httpStatusCode: Int,
    val errorCode: String? = null,
    errorDescription: String? = null
) : RuntimeException(errorDescription)
