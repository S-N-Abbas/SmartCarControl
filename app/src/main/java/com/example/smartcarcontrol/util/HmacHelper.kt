package com.example.smartcarcontrol.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacHelper {

    fun sign(message: String, secretHex: String): String {
        val keyBytes = hexToBytes(secretHex)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val result = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytesToHex(result)
    }

    fun verify(message: String, signature: String, secretHex: String): Boolean {
        val expected = sign(message, secretHex)
        return expected.equals(signature, ignoreCase = true)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}