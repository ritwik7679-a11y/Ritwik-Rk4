package com.example.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallActionHandler(private val context: Context) {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    /**
     * Resolves contact name from a given phone number
     */
    fun resolveContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        try {
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CallActionHandler", "Failed to resolve contact name: ${e.message}")
        }
        return null
    }

    /**
     * Search contacts by name query to retrieve phone number
     */
    fun searchContactPhoneNumber(nameQuery: String): Pair<String, String>? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (nameIdx != -1 && numIdx != -1) {
                        val name = it.getString(nameIdx)
                        val number = it.getString(numIdx)
                        return Pair(name, number)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CallActionHandler", "Failed to search contact: ${e.message}")
        }
        return null
    }

    /**
     * Answers ringing call via TelecomManager
     */
    @SuppressLint("MissingPermission")
    fun answerRingingCall(): Result<String> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return Result.failure(
                SecurityException("कॉल उठाने के लिए परमिशन ज़रूरी है। कृपया सेटिंग्स में अनुमति दें।")
            )
        }

        if (telecomManager == null) {
            return Result.failure(IllegalStateException("इस फोन पर टेलीकॉम सेवा उपलब्ध नहीं है।"))
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager.acceptRingingCall()
                Result.success("कॉल उठा ली गई है।")
            } else {
                Result.failure(UnsupportedOperationException("वॉयस से कॉल उठाना एंड्रॉयड 8.0 या नए वर्शन पर समर्थित है।"))
            }
        } catch (e: Exception) {
            Log.e("CallActionHandler", "Failed to answer call: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Rejects or ends the incoming/active call via TelecomManager
     */
    @SuppressLint("MissingPermission")
    fun rejectRingingCall(): Result<String> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return Result.failure(
                SecurityException("कॉल काटने के लिए परमिशन ज़रूरी है।")
            )
        }

        if (telecomManager == null) {
            return Result.failure(IllegalStateException("टेलीकॉम सेवा उपलब्ध नहीं है।"))
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val ended = telecomManager.endCall()
                if (ended) {
                    Result.success("कॉल काट दी गई है।")
                } else {
                    Result.failure(IllegalStateException("कॉल बंद नहीं हो सकी।"))
                }
            } else {
                Result.failure(UnsupportedOperationException("वॉयस से कॉल काटना एंड्रॉयड 9.0 या नए वर्शन पर समर्थित है।"))
            }
        } catch (e: Exception) {
            Log.e("CallActionHandler", "Failed to reject call: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Initiates a phone call
     */
    @SuppressLint("MissingPermission")
    fun makePhoneCall(phoneNumber: String): Result<String> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return Result.failure(
                SecurityException("फोन कॉल लगाने के लिए कॉल परमिशन ज़रूरी है।")
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("$phoneNumber पर कॉल लगाई जा रही है...")
        } catch (e: Exception) {
            Log.e("CallActionHandler", "Failed to place call: ${e.message}")
            Result.failure(e)
        }
    }
}
