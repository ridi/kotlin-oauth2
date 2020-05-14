package com.ridi.oauth2

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface ApiService {
    companion object {
        const val PASSWORD_GRANT_TYPE = "password"
        const val REFRESH_TOKEN_GRANT_TYPE = "refresh_token"
    }

    @POST("oauth2/token")
    @FormUrlEncoded
    fun requestToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("username") username: String?,
        @Field("password") password: String?,
        @Field("refresh_token") refreshToken: String?,
        @FieldMap extraData: Map<String, String>
    ): Call<TokenResponse>
}
