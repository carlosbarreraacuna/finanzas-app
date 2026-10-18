package com.carlos.finanzas.backup

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Formato: MAGIC(6) | salt(16) | iv(12) | ciphertext+tag(GCM).
 * Clave: PBKDF2WithHmacSHA256 (210.000 iteraciones) derivada de la contraseña del usuario.
 * El MAGIC se autentica como datos adicionales (AAD).
 */
object BackupCrypto {
    private val MAGIC = "FNZBK1".toByteArray(Charsets.US_ASCII)
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 210_000

    class InvalidBackupException(message: String) : Exception(message)

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(SALT_LEN).also(rnd::nextBytes)
        val iv = ByteArray(IV_LEN).also(rnd::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(MAGIC)
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    /** @throws InvalidBackupException si el archivo no es un backup o la contraseña es incorrecta. */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        val headerLen = MAGIC.size + SALT_LEN + IV_LEN
        if (data.size <= headerLen || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw InvalidBackupException("El archivo no es un backup válido")
        }
        val salt = data.copyOfRange(MAGIC.size, MAGIC.size + SALT_LEN)
        val iv = data.copyOfRange(MAGIC.size + SALT_LEN, headerLen)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(MAGIC)
            cipher.doFinal(data, headerLen, data.size - headerLen)
        } catch (e: GeneralSecurityException) {
            throw InvalidBackupException("Contraseña incorrecta o archivo dañado")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
