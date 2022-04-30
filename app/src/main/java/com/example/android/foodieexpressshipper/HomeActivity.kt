package com.example.android.foodieexpressshipper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.android.foodieexpressshipper.common.Common
import com.example.android.foodieexpressshipper.ui.home.HomeFragment
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)
        updateToken()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HomeFragment.newInstance())
                .commitNow()
        }
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