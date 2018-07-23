package com.ridi.oauth2

import android.util.Base64
import android.util.Base64.encodeToString
import java.io.UnsupportedEncodingException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val ivSpec = IvParameterSpec(ByteArray(16))
private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

// AES only supports key sizes of 16, 24 or 32 bytes

@Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class, NoSuchPaddingException::class,
    InvalidKeyException::class, InvalidAlgorithmParameterException::class, IllegalBlockSizeException::class,
    BadPaddingException::class)
internal fun String.encodeWithAES256(key: String?): String {
    if (key == null) return this

    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
    return encodeToString(cipher.doFinal(this.toByteArray(Charsets.UTF_8)), 0)
}

// AES only supports key sizes of 16, 24 or 32 bytes

@Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class, NoSuchPaddingException::class,
    InvalidKeyException::class, InvalidAlgorithmParameterException::class, IllegalBlockSizeException::class,
    BadPaddingException::class)
internal fun String.decodeWithAES256(key: String?): String {
    if (key == null) return this

    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
    return String(cipher.doFinal(Base64.decode(this, 0)), Charsets.UTF_8)
}
