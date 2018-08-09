package com.ridi.oauth2

import android.util.Base64
import android.util.Base64.encodeToString
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val ivSpec = IvParameterSpec(ByteArray(16))
private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

// AES only supports key sizes of 16, 24 or 32 bytes

internal fun String.encodeWithAES128(key: String?): String {
    if (key == null) {
        return this
    }

    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
    return encodeToString(cipher.doFinal(this.toByteArray(Charsets.UTF_8)), 0)
}

// AES only supports key sizes of 16, 24 or 32 bytes

internal fun String.decodeWithAES128(key: String?): String {
    if (key == null) {
        return this
    }

    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
    return String(cipher.doFinal(Base64.decode(this, 0)), Charsets.UTF_8)
}
