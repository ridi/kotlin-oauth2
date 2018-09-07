package com.ridi.oauth2

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
private const val IV_SIZE = 16

private fun String.toSecretKeySpec() = SecretKeySpec(toByteArray(), "AES")

internal fun String.encodeWithAES128(key: String?): String {
    key ?: return this

    val cipher = Cipher.getInstance(TRANSFORMATION)
    val iv = ByteArray(IV_SIZE)
    SecureRandom().nextBytes(iv)
    cipher.init(Cipher.ENCRYPT_MODE, key.toSecretKeySpec(), IvParameterSpec(iv))
    return Base64.encodeToString(iv + cipher.doFinal(toByteArray()), 0)
}

internal fun String.decodeWithAES128(key: String?): String {
    key ?: return this

    val byteArray = Base64.decode(this, 0)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key.toSecretKeySpec(), IvParameterSpec(byteArray, 0, IV_SIZE))
    return String(cipher.doFinal(byteArray, IV_SIZE, byteArray.size - IV_SIZE))
}
