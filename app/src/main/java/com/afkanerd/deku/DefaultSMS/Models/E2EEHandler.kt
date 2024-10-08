package com.afkanerd.deku.DefaultSMS.Models

import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.BuildConfig
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.KeystoreHelpers
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityAES
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityCurve25519
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.SecurityRSA
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Headers
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory


object E2EEHandler {
    private const val HYBRID_KEYS_FILE = ".com.afkanerd.dekusms.HYBRID_KEYS_FILE"

    enum class MagicNumber(val num: Int) {
        REQUEST(0),
        ACCEPT(1),
        MESSAGE(2)
    }

    private fun getSharedPreferenceFilename(address: String): String {
        return address + HYBRID_KEYS_FILE;
    }

    private fun secureStorePrivateKey(context: Context,
                                      address: String,
                                      keystoreAlias: String,
                                      encryptedCipherPrivateKey: ByteArray) {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit()
            .putString(keystoreAlias, Base64.encodeToString(encryptedCipherPrivateKey,
                Base64.DEFAULT)).apply()
    }

    fun secureStorePeerPublicKey(context: Context, keystoreAlias: String,
                                 publicKey: ByteArray) {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(keystoreAlias),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit()
            .putString(derivePeerPublicKeystoreAlias(keystoreAlias),
                Base64.encodeToString(publicKey, Base64.DEFAULT)).apply()
    }

    fun secureFetchPeerPublicKey(context: Context, keystoreAlias: String) : String {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(keystoreAlias),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).getString(derivePeerPublicKeystoreAlias(keystoreAlias), "")!!
    }

    fun generateKey(context: Context, address: String): ByteArray {
        val libSigCurve25519 = SecurityCurve25519()
        val publicKey = libSigCurve25519.generateKey()

        if(isSelf(context, address)) {
            val encryptionPublicKey = SecurityRSA.generateKeyPair(
                deriveSelfSecureRequestKeystoreAlias(address), 2048)
            val privateKeyCipherText = SecurityRSA.encrypt(encryptionPublicKey,
                libSigCurve25519.privateKey)
            secureStorePrivateKey(
                context, address, deriveSelfSecureRequestKeystoreAlias(address),
                privateKeyCipherText
            )
        }
        else {
            val encryptionPublicKey = SecurityRSA.generateKeyPair(
                deriveSecureRequestKeystoreAlias(address), 2048)
            val privateKeyCipherText = SecurityRSA.encrypt(encryptionPublicKey,
                libSigCurve25519.privateKey)
            secureStorePrivateKey(context, address, deriveSecureRequestKeystoreAlias(address),
                privateKeyCipherText)
        }
        return publicKey
    }

    private fun getSecuredStoredPrivateKey(context: Context, address: String,
                                           isSelf: Boolean= false) : String {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return if(isSelf)
            sharedPreferences.getString(deriveSecureRequestKeystoreAlias(address), "")!!
        else sharedPreferences.getString(deriveSecureRequestKeystoreAlias(address), "")!!
    }


    private fun fetchPrivateKey(context: Context, address: String) : ByteArray {
        val cipherPrivateKeyString = getSecuredStoredPrivateKey(context, address)
        if(cipherPrivateKeyString.isBlank()) {
            throw Exception("Cipher private key is empty...")
        }

        val cipherPrivateKey = Base64.decode(cipherPrivateKeyString, Base64.DEFAULT)
        val keypair = KeystoreHelpers.getKeyPairFromKeystore(deriveSecureRequestKeystoreAlias(address))
        return SecurityRSA.decrypt(keypair.private, cipherPrivateKey)
    }

    fun calculateSharedSecret(context: Context, address: String, publicKey: ByteArray): ByteArray {
        val privateKey = fetchPrivateKey(context, address)
        val libSigCurve25519 = SecurityCurve25519(privateKey)
        return libSigCurve25519.calculateSharedSecret(publicKey)
    }

    fun formatRequestPublicKey(publicKey: ByteArray, magicNumber: MagicNumber) : ByteArray {
        val mn: ByteArray = byteArrayOf(magicNumber.num.toByte())
        val lenPubKey = ByteArray(4)
        ByteBuffer.wrap(lenPubKey).order(ByteOrder.LITTLE_ENDIAN).putInt(publicKey.size)

        return mn + lenPubKey + publicKey
    }

    fun formatMessage(header: Headers, cipherText: ByteArray) : ByteArray {
        val mn: ByteArray = byteArrayOf(MagicNumber.MESSAGE.num.toByte())
        val lenHeader = ByteArray(4)
        ByteBuffer.wrap(lenHeader).order(ByteOrder.LITTLE_ENDIAN).putInt(header.serialized.size)

        val lenMsg = ByteArray(4)
        ByteBuffer.wrap(lenMsg).order(ByteOrder.LITTLE_ENDIAN).putInt(cipherText.size)

        return mn + lenHeader + lenMsg + header.serialized + cipherText
    }

    fun isValidMessage(data: ByteArray) : Boolean {
        val magicNumber: Int = data[0].toInt()
        val lenHead = ByteArray(4)
        System.arraycopy(data, 1, lenHead, 0, 4)
        val lenHeader = ByteBuffer.wrap(lenHead).order(ByteOrder.LITTLE_ENDIAN).getInt()

        val lenMsg = ByteArray(4)
        System.arraycopy(data, 5, lenMsg, 0, 4)
        val lenMessage = ByteBuffer.wrap(lenMsg).order(ByteOrder.LITTLE_ENDIAN).getInt()

        // TODO: wild guess
        // TODO: check len header as well
        return magicNumber == MagicNumber.MESSAGE.num && lenMessage % 16 == 0
    }

    fun extractPublicKeyFromPayload(data: ByteArray): ByteArray {
        val magicNumber: Int = data[0].toInt()
        val lenPubKey = ByteArray(4)
        System.arraycopy(data, 1, lenPubKey, 0, 4)
        val lenPublicKey = ByteBuffer.wrap(lenPubKey).order(ByteOrder.LITTLE_ENDIAN).getInt()

        val pubKey = ByteArray(lenPublicKey)
        System.arraycopy(data, 5, pubKey, 0, pubKey.size)

        return pubKey
    }

    fun isValidPublicKey(data: ByteArray) : Boolean {
        val magicNumber: Int = data[0].toInt()

        val lenPubKey = ByteArray(4)
        System.arraycopy(data, 1, lenPubKey, 0, 4)
        val lenPublicKey = ByteBuffer.wrap(lenPubKey).order(ByteOrder.LITTLE_ENDIAN).getInt()

        // TODO: Can validate based on length
        val pubKey = ByteArray(lenPublicKey)
        System.arraycopy(data, 5, pubKey, 0, pubKey.size)

        // TODO: wild guess - but would allow for bad public keys, be more strict for now
//        return (magicNumber == MagicNumber.REQUEST.num || magicNumber == MagicNumber.ACCEPT.num)
//                && lenPublicKey % 4 == 0
        return (magicNumber == MagicNumber.REQUEST.num || magicNumber == MagicNumber.ACCEPT.num)
                && lenPublicKey == 32
    }

    private fun deriveSelfSecureRequestKeystoreAlias(address: String) : String{
        return address + "_self_secure_request"
    }
    private fun deriveSecureRequestKeystoreAlias(address: String) : String{
        return address + "_secure_request"
    }

    private fun derivePeerPublicKeystoreAlias(address: String) : String{
        return address + "_peer_public_key"
    }

    private fun deriveSaveStatesKeystoreAlias(address: String) : String{
        return address + "_states"
    }

    private fun deriveSelfSaveStatesKeystoreAlias(address: String) : String{
        return address + "_self_states"
    }

    private fun deriveSaveStatesEncryptKeystoreAlias(address: String) : String{
        return address + "_secure_states"
    }

    private fun deriveSelfSaveStatesEncryptKeystoreAlias(address: String) : String{
        return address + "_self_secure_states"
    }

    fun getRequestType(data: ByteArray) : MagicNumber? {
        val magicNumber: Int = data[0].toInt()
        var enumMagicNumber : MagicNumber? = null

        MagicNumber.entries.forEach {
            if(it.num == magicNumber) {
                enumMagicNumber = it
                return@forEach
            }
        }
        return enumMagicNumber
    }

    /**
     * Generate keys before calling this method
     */
   fun isSelf(context: Context, address: String): Boolean {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return sharedPreferences.contains("is_self") &&
                sharedPreferences.getBoolean("is_self", false)
    }

    fun makeSelfRequest(context: Context, address: String) {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().putBoolean("is_self", true)
            .apply()
    }

    fun hasRequest(context: Context, address: String): Boolean {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences =  EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return sharedPreferences.contains(deriveSecureRequestKeystoreAlias(address))
    }

    fun hasPendingApproval(context: Context, address: String): Boolean {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences =  EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return if(isSelf(context, address))
            sharedPreferences.contains(derivePeerPublicKeystoreAlias(address)) &&
                !sharedPreferences.contains(deriveSelfSecureRequestKeystoreAlias(address))
        else sharedPreferences.contains(derivePeerPublicKeystoreAlias(address)) &&
                !sharedPreferences.contains(deriveSecureRequestKeystoreAlias(address))
    }

    fun isSecured(context: Context, address: String): Boolean {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences =  EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )


        return if(isSelf(context, address))
            sharedPreferences.contains(derivePeerPublicKeystoreAlias(address)) &&
                sharedPreferences.contains(deriveSelfSecureRequestKeystoreAlias(address))
        else sharedPreferences.contains(derivePeerPublicKeystoreAlias(address)) &&
                sharedPreferences.contains(deriveSecureRequestKeystoreAlias(address))
    }

    fun clear(context: Context, address: String) {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences =  EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().clear().apply()
        if(BuildConfig.DEBUG)
            Toast.makeText(context, "Cleared Artifacts for: $address", Toast.LENGTH_SHORT)
                .show()
    }

    fun storeState(context: Context, state: String, address: String, isSelf: Boolean = false) {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences =  EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val secretKey = SecurityAES.generateSecretKey(256)
        val stateCipherText = Base64.encodeToString(SecurityAES
            .encryptAES256CBC(state.encodeToByteArray(), secretKey.encoded, null),
            Base64.DEFAULT)

        if(isSelf) {
            val encryptionPublicKey = SecurityRSA.generateKeyPair(
                deriveSelfSaveStatesEncryptKeystoreAlias(address), 2048)

            sharedPreferences.edit()
                .putString(deriveSelfSaveStatesKeystoreAlias(address), stateCipherText)
                .putString("self_secret_key",
                    Base64.encodeToString(SecurityRSA.encrypt(encryptionPublicKey, secretKey.encoded),
                        Base64.DEFAULT))
                .apply()
        } else {
            val encryptionPublicKey = SecurityRSA.generateKeyPair(
                deriveSaveStatesEncryptKeystoreAlias(address), 2048)

            sharedPreferences.edit()
                .putString(deriveSaveStatesKeystoreAlias(address), stateCipherText)
                .putString("secret_key",
                    Base64.encodeToString(SecurityRSA.encrypt(encryptionPublicKey, secretKey.encoded),
                        Base64.DEFAULT))
                .apply()
        }
    }

    fun fetchStates(context: Context, address: String, isSelf: Boolean = false): String {
        val masterKey: MasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            getSharedPreferenceFilename(address),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

         val encodedEncryptedState = if(!isSelf)
             sharedPreferences.getString(deriveSaveStatesKeystoreAlias(address), "")!!
        else sharedPreferences.getString(deriveSelfSaveStatesKeystoreAlias(address), "")!!

        if(encodedEncryptedState.isNullOrBlank())
            return ""

        val keypair = if(isSelf) KeystoreHelpers.getKeyPairFromKeystore(
            deriveSelfSaveStatesEncryptKeystoreAlias(address))
        else KeystoreHelpers.getKeyPairFromKeystore(deriveSaveStatesEncryptKeystoreAlias(address))

        val secretKey = SecurityRSA.decrypt(keypair.private,
            Base64.decode(encodedEncryptedState, Base64.DEFAULT))
        return String(SecurityAES.decryptAES256CBC(Base64.decode(encodedEncryptedState,
            Base64.DEFAULT), secretKey, null), Charsets.UTF_8)
    }
}