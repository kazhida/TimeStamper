package com.abplus.timestamper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.abplus.timestamper.models.TimeStamp
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        private const val REQUEST_SIGN_IN = 5445
        private const val TIME_STAMPS = "time_stamps"

    }

    private val rootView: View by lazy { findViewById<View>(R.id.root_view) }
    private val toolbar: Toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val fab: FloatingActionButton by lazy { findViewById<FloatingActionButton>(R.id.fab) }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val store: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val uid: String? get() = auth.currentUser?.uid
    private var busy: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            postTimeStamp()
        }

        if (isTimestampScheme(intent?.data)) {
            postTimeStamp()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode != Activity.RESULT_OK) {
                // サインインできなかったので終わる
                Toast.makeText(this, R.string.err_cannot_signin, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (isTimestampScheme(intent?.data)) {
            postTimeStamp()
        }
    }

    override fun onResume() {
        super.onResume()

        if (uid == null) {
            signIn()
        }
    }

    @Synchronized
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        busy = false

        // つながらなかったので終わる
        Toast.makeText(this, connectionResult.errorMessage, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun signIn() {
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
            .build()
        startActivityForResult(intent, REQUEST_SIGN_IN)
    }

    private fun isTimestampScheme(uri: Uri?): Boolean = (uri?.scheme == getString(R.string.intent_scheme))

    @Synchronized
    private fun postTimeStamp() {
        busy = true
        val timeStamp = uid?.let { TimeStamp(it) }
        if (timeStamp != null) {
            store.collection(TIME_STAMPS)
                .add(timeStamp.data)
                .addOnCompleteListener {
                    task -> onComplete(task)
                }
        }
    }

    @Synchronized
    private fun onComplete(task: Task<DocumentReference>) {
        busy = false
        if (task.isSuccessful) {
            Snackbar.make(rootView, "", Snackbar.LENGTH_LONG)
                .setAction(R.string.close) { finish() }
                .show()
        } else {
            // 記録できなかったので終わる
            Toast.makeText(this, R.string.err_cannot_signin, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
