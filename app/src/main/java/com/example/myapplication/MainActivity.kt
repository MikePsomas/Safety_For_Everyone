package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_PERMISSION = 1
    private val REQUEST_CONTACT_PERMISSION = 2
    private lateinit var contact1EditText: EditText
    private lateinit var contact2EditText: EditText
    private lateinit var sosButton: Button
    private lateinit var mediaPlayer: MediaPlayer
    private val PICK_CONTACT = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contact1EditText = findViewById(R.id.contact1)
        contact2EditText = findViewById(R.id.contact2)
        sosButton = findViewById(R.id.sendSOSButton)
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)

        sosButton.setOnClickListener {
            requestLocationPermission()
            mediaPlayer.start()
        }

        val stopSoundButton: Button = findViewById(R.id.stopButton)
        stopSoundButton.setOnClickListener {
            if (mediaPlayer.isPlaying)
                mediaPlayer.stop()
        }

        contact1EditText.setOnClickListener {
            requestContactPermission()
        }

        contact2EditText.setOnClickListener {
            requestContactPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun requestContactPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACT_PERMISSION
            )
        } else {
            pickContact()
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    sendSOS(location)
                } ?: run {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to get location: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendSOS(location: Location) {
        val phoneNumber1 = contact1EditText.text.toString()
        val phoneNumber2 = contact2EditText.text.toString()

        if (phoneNumber1.isNotEmpty()) {
            sendSMSWithLocation(phoneNumber1, location)
        }
        if (phoneNumber2.isNotEmpty()) {
            sendSMSWithLocation(phoneNumber2, location)
        }
    }

    private fun sendSMSWithLocation(phoneNumber: String, location: Location) {
        val mapsUrl = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
        val smsMessage =
            "Emergency! My current location is: ${location.latitude}, ${location.longitude}. Click to view on Google Maps: $mapsUrl"

        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, smsMessage, null, null)
            Toast.makeText(this, "SOS sent successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SOS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, PICK_CONTACT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_CONTACT_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact()
            } else {
                Toast.makeText(this, "Contact permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            data?.data?.let { contactUri ->
                val cursor = contentResolver.query(contactUri, null, null, null, null)
                cursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val hasPhoneNumber =
                            cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                        if (hasPhoneNumber > 0) {
                            val id =
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                            val phoneCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            phoneCursor?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val phoneNumber =
                                        phoneCursor.getString(
                                            phoneCursor.getColumnIndex(
                                                ContactsContract.CommonDataKinds.Phone.NUMBER
                                            )
                                        )

                                    if (contact1EditText.hasFocus()) {
                                        contact1EditText.setText(phoneNumber)
                                    } else if (contact2EditText.hasFocus()) {
                                        contact2EditText.setText(phoneNumber)
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Selected contact does not have a phone number",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
