package com.ridi.oauth2

class UnexpectedResponseException(val responseCode: Int) : RuntimeException()
