package com.example.android.foodieexpressshipper

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.android.foodieexpressshipper.common.Common
import com.example.android.foodieexpressshipper.ui.home.HomeFragment
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging
import io.paperdb.Paper

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)
        updateToken()
        checkStartTrip()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HomeFragment.newInstance())
                .commitNow()
        }
    }

    override fun onResume() {
        super.onResume()
        checkStartTrip()
    }

    private fun checkStartTrip() {
        Paper.init(this)
        val data = Paper.book().read<String>(Common.TRIP_START)
        if(!TextUtils.isEmpty(data))
            startActivity(Intent(this,ShippingActivity::class.java))
    }

    private fun updateToken() {
        FirebaseMessaging.getInstance().token
            .addOnFailureListener {
                e-> Toast.makeText(this@HomeActivity,""+e.message,Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { instanceIdResult ->
                Common.updateToken(this@HomeActivity,instanceIdResult,false,true)
            }
    }
}