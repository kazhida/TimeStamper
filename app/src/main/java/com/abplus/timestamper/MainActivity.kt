package com.abplus.timestamper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.abplus.timestamper.models.TimeStamp
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        private const val REQUEST_SIGN_IN = 5445
        private const val TIME_STAMPS = "time_stamps"
    }

    private val rootView: View by lazy { findViewById<View>(R.id.root_view) }
    private val toolbar: Toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val fab: FloatingActionButton by lazy { findViewById<FloatingActionButton>(R.id.fab) }
    private val refreshLayout: SwipeRefreshLayout by lazy { findViewById<SwipeRefreshLayout>(R.id.refresh_layout) }
    private val listView: ExpandableListView by lazy { findViewById<ExpandableListView>(R.id.list_view) }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val store: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val uid: String? get() = auth.currentUser?.uid
    private var busy: Boolean = false

    private val dateFormatter by lazy { android.text.format.DateFormat.getDateFormat(applicationContext) }
    private val timeFormatter by lazy { android.text.format.DateFormat.getTimeFormat(applicationContext) }

    private val adapter by lazy { TimeStampAdapter() }

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

        listView.setAdapter(adapter)
        listView.setGroupIndicator(resources.getDrawable(R.drawable.ic_arrow_black_24dp, null))

        refreshLayout.setOnRefreshListener {
            reload(uid)
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
        } else {
            reload(uid)
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
                    task -> onComplete(task, timeStamp)
                }
        }
    }

    @Synchronized
    private fun onComplete(task: Task<DocumentReference>, timeStamp: TimeStamp) {
        busy = false
        if (task.isSuccessful) {
            Snackbar.make(rootView, getString(R.string.notification_stamped, timeStamp.asTime), Snackbar.LENGTH_LONG)
                .setAction(R.string.close) { finish() }
                .show()
            reload(auth.currentUser?.uid)
        } else {
            // 記録できなかったので終わる
            Toast.makeText(this, R.string.err_cannot_signin, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun reload(uid: String?) {
        if (uid != null) {
            store.collection(TIME_STAMPS)
                .whereLessThan(uid, Timestamp.now())
                .orderBy(uid, Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .addOnCompleteListener { task ->
                    refreshLayout.isRefreshing = false
                    if (task.isSuccessful) {
                        task.result?.documents?.mapNotNull {
                            val now = it.getTimestamp(uid)
                            if (now != null) {
                                TimeStamp(uid, now)
                            } else {
                                null
                            }
                        }?.let {
                            adapter.reset(it)
                        }
                    } else {
                        Toast.makeText(this, R.string.err_cannot_load, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private inner class TimeStampAdapter : BaseExpandableListAdapter() {

        private val items = ArrayList<TimeStamp.Grouped>()

        fun reset(stamps: List<TimeStamp>) {
            items.clear()
            items.addAll(TimeStamp.grouping(stamps))
            notifyDataSetChanged()
            if (items.isNotEmpty()) {
                listView.expandGroup(0, true)
            }
        }

        override fun getGroup(groupPosition: Int): Any = items[groupPosition]

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

        override fun hasStableIds(): Boolean = true

        override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.view_time_group, parent, false)

            view.findViewById<TextView>(R.id.text_view).let {
                val date = items[groupPosition].date
                it.text = dateFormatter.format(date.time)
                it.isSelected = isExpanded
            }

            return view
        }

        override fun getChildrenCount(groupPosition: Int): Int = items[groupPosition].times.size

        override fun getChild(groupPosition: Int, childPosition: Int): Any = items[groupPosition].times[childPosition]

        override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

        override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup?
        ): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.view_time_stamp, parent, false)

            view.findViewById<TextView>(R.id.text_view).let {
                val time = items[groupPosition].times[childPosition]
                it.text = timeFormatter.format(time.time)
            }

            return view
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

        override fun getGroupCount(): Int = items.size
    }

    private val TimeStamp.asTime: String get() = timeFormatter.format(now.toDate())
}
