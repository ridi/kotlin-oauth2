package com.ridi.oauth2

import android.util.Base64
import android.util.Base64.encodeToString
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val ivSpec = IvParameterSpec(ByteArray(16))

internal fun String.encodeWithAES128(key: String?): String {
    if (key == null) {
        return this
    }

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
    return encodeToString(cipher.doFinal(this.toByteArray(Charsets.UTF_8)), 0)
}

internal fun String.decodeWithAES128(key: String?): String {
    if (key == null) {
        return this
    }

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
    return String(cipher.doFinal(Base64.decode(this, 0)), Charsets.UTF_8)
}