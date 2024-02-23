package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.logging.Handler


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), ShakeDetector.OnShakeListener {

    private val PERMISSION_REQUEST_CODE = 123
    private val REQUEST_LOCATION_PERMISSION = 1
    private val REQUEST_CONTACT_PERMISSION_FOR_SAVING = 2
    private val contactList = ArrayList<String>()
    private lateinit var contactNameEditText: EditText
    private lateinit var contactNumberEditText: EditText
    private lateinit var sosButton: Button
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var saveNumberButton: Button
    private lateinit var deleteContactButton: Button
    private lateinit var contactRecyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    private lateinit var emergencyContactIcon: ImageView
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var audioManager: AudioManager
    private var hasNumber = false
    private var doubleBackToExitPressedOnce = false


    override fun onBackPressed() {
        // Check if the double back press is set to true
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed() // If true then proceed with the default back button behavior (to exit the app)
            return
        }
        // Set double back press to true and show the message
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show()

        // Use the Handler to delay and resetting double back press to false
        android.os.Handler(mainLooper).postDelayed({ doubleBackToExitPressedOnce = false},
            2000) // Delay for 2 seconds
    }

    // Use of Activity Result Contacts to handle contact picking
    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { contactUri ->
                    val cursor = contentResolver.query(contactUri, null, null, null, null)
                    cursor?.use { outerCursor ->
                        // We Retrieve the contact information from here!
                        val idColumnIndex =
                            outerCursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameColumnIndex =
                            outerCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                        if (idColumnIndex != -1 && nameColumnIndex != -1) {
                            if (outerCursor.moveToFirst()) {
                                val contactId = outerCursor.getString(idColumnIndex)
                                val displayName = outerCursor.getString(nameColumnIndex)

                                // We Retrieve the phone number that is associated with the contact
                                val phoneCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(contactId),
                                    null
                                )

                                phoneCursor?.use { innerCursor ->
                                    val phoneNumberColumnIndex =
                                        innerCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (phoneNumberColumnIndex != -1) {
                                        if (innerCursor.moveToFirst()) {
                                            val phoneNumber =
                                                innerCursor.getString(phoneNumberColumnIndex)
                                            saveContactNumber("$displayName: $phoneNumber")
                                        } else {
                                            //Inform the user that this contact has no phone number is empty
                                            Toast.makeText(this,
                                                "Selected contact does not have a phone number",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        // Inform the user if this contact number has not been found
                                        Toast.makeText(
                                            this,
                                            "Phone number column not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            // Inform the user if this contact does not have a contact id or it does not display a name.
                            Toast.makeText(
                                this,
                                "Contact ID or Display Name column not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

    //Adjust the volume for media player!
    private fun adjustVolumeForMediaPlayer() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }
    //Adjust the volume for audio manager
    private fun adjustVolumeAndState() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_RING,
            am.getStreamMaxVolume(AudioManager.STREAM_RING),
            0
        )
        am.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }
    //Setting up the activity's UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views and buttons
        sosButton = findViewById(R.id.sendSOSButton)
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        contactNameEditText = findViewById(R.id.contactNameEditText)
        contactNumberEditText = findViewById(R.id.contactNumberEditText)
        saveNumberButton = findViewById(R.id.saveNumberButton)
        deleteContactButton = findViewById(R.id.deleteContactButton)
        contactRecyclerView = findViewById(R.id.contactRecyclerView)
        emergencyContactIcon = findViewById(R.id.emergencyContactsIcon)

        // We Request necessary permissions here!
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)

        // Initialize the shake detector
        shakeDetector = ShakeDetector(this)
        shakeDetector.setOnShakeListener(this)

        // Set up the RecyclerView for the contacts we gonna show to the user
        adapter = ContactAdapter(contactList)
        contactRecyclerView.adapter = adapter
        contactRecyclerView.layoutManager = LinearLayoutManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        //This is our contact icon
        emergencyContactIcon.setImageResource(R.drawable.baseline_contacts_24)
        //When the user press this the click listener will sent the user to its contacts
        emergencyContactIcon.setOnClickListener {
            //Request the permission for saving
            requestContactPermissionForSaving()
        }
        // Set click listener for the button responsible for saving and entering contact number!
        saveNumberButton.setOnClickListener {
            saveEnteredNumber() // call the function to save and enter the contact number
        }
        // Set on click listener for the SOS button
        sosButton.setOnClickListener {
            // Check if user entered a contact number
            if (hasNumber) {
                requestLocationPermission() // Request location permission
                adjustVolumeForMediaPlayer() // Adjust the volume for media player
                mediaPlayer.start() //  Start media player
            } else {
                //Inform the user to enter a contact number if they haven't
                Toast.makeText(this, "Please enter a contact number", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize and set on click listener for the button
        //This button is responsible for stopping the alarm sound
        val stopSoundButton: Button = findViewById(R.id.stopButton)
        stopSoundButton.setOnClickListener {
            // Check if the media player is currently playing
            if (mediaPlayer.isPlaying)
                mediaPlayer.stop() // stop media player
        }
        // Set on click listener for the button
        //This button is responsible for deleting the contacts
        deleteContactButton.setOnClickListener {
            // Remove all contacts from the adapter
            //and  reset the hasNumber flag
            adapter.clearItems()
            // that helps the system understand
            // if there is a number stored in the app
            hasNumber = false
        }
    }
    // handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //check when the app asks for permissions and if they are accepted.
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // check if all permissions were granted
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, adjust the volume for media player
                adjustVolumeForMediaPlayer()
            } else {
                //Display the message that permissions were denied
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // On shake called when shake is detected
    override fun onShake(count: Int) {
        // Adjust ring volume to maximum
        audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
            0
        )
        // Adjust media volume to maximum
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
        )
        // Display message indicating that shake detection and SOS was send
        Toast.makeText(
            this@MainActivity,
            "Shake Detector is activated and the SOS was sent",
            Toast.LENGTH_SHORT
        ).show()

        // request location permission so it can send the SOS
        requestLocationPermission()
    }
    // Request permission for accessing contacts to be saved as emergency contacts
    private fun requestContactPermissionForSaving() {
        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission to read the contacts
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACT_PERMISSION_FOR_SAVING
            )
        } else { // If permission is already granted, pick a contact
            pickContactForSaving()
        }
    }
    // Pick contacts for saving in an emergency contact
    private fun pickContactForSaving() {
        // Create an intent so it can pick a contact from the contact app
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        // Launch the activity result launcher
        pickContactLauncher.launch(intent)
    }
    // Get the current device location
    private fun getCurrentLocation() {
        // Getting the device's last know location
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        // Check if the location permissions are granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Return if location permissions are not granted
            return
        }
        // Retrieve the device's last know location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // If location is obtained then we send the SOS location
                location?.let {
                    sendSOS(location)
                } ?: run {
                    // If the location is null then we display that receiving the location failed.
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
            }
            // If an exception occurs while receiving the location, display the message
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
    // Request location permission
    private fun requestLocationPermission() {
        // Check if location is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Adjust volume and get the current location if permission is granted
            adjustVolumeAndState()
            getCurrentLocation()
        }
    }
    // Send SOS with the current location
    private fun sendSOS(location: Location) {
        // Get the first contact number from the list
        val phoneNumber1 = contactList.firstOrNull()
        // Adjust the phones volume to maximum
        audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
            0
        )
        // Check if a contact number is available
        if (phoneNumber1 != null) {
            //Stop media player if button pressed
            //Start media player if button pressed
            mediaPlayer.stop()
            mediaPlayer.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
            mediaPlayer.start()
            // Send SMS with location to the contact number
            sendSMSWithLocation(phoneNumber1, location)
        } else {
            // Display a message if no contact number is available
            Toast.makeText(this, "Please enter a contact number", Toast.LENGTH_SHORT).show()
        }
    }
    // Send SMS with current location
    private fun sendSMSWithLocation(phoneNumber: String, location: Location) {
        // Creates the Google Maps URL with the current latitude and longitude
        val mapsUrl = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
        // Creates the SOS message with the current latitude and longitude and Google Maps
        val smsMessage =
            "Emergency! My current location is: ${location.latitude}, ${location.longitude}. Click to view on Google Maps: $mapsUrl"

        try {
            // Get the default SMS manager and send the SMS message
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, smsMessage, null, null)
            // Display a message indicating successful SOS sending
            Toast.makeText(this, "SOS sent successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Handle SMS sending errors and displaying an error message
            Toast.makeText(this, "Failed to send SOS: ${e.message}", Toast.LENGTH_SHORT).show()

        }
    }
    // Save a contact number to the list of emergency contacts
    private fun saveContactNumber(phoneNumber: String) {
        // Add phone number in the contact list
        contactList.add(phoneNumber)
        // Notify the adapter that an item has been inserted
        adapter.notifyItemInserted(contactList.size - 1)
        // Set the hasNumber flag to true indicating a if a contact number is saved
        hasNumber = true
    }
    // Save the entered number as an emergency contact
    private fun saveEnteredNumber() {
        // Get the entered name and number from EditText
        val enteredName = contactNameEditText.text.toString().trim()
        val enteredNumber = contactNumberEditText.text.toString().trim()
        // Check if either name or number is entered
        when {
            enteredNumber.isNotEmpty() -> saveContactNumber("$enteredName: $enteredNumber")
            enteredName.isNotEmpty() -> saveContactNumber(enteredName)
            else -> Toast.makeText(this, "Please enter a name or number", Toast.LENGTH_SHORT).show()
        }
        // Clear both EditText fields after saving
        contactNameEditText.text.clear()
        contactNumberEditText.text.clear()
    }
}
