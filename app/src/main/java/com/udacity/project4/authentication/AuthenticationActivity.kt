package com.udacity.project4.authentication

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionProvider
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity

class AuthenticationActivity : AppCompatActivity() {

    val AUTH_REQUEST_CODE = 7081
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var listener: FirebaseAuth.AuthStateListener
    lateinit var providers: List<AuthUI.IdpConfig>

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

//Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
//If the user was authenticated, send him to RemindersActivity

        /*val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)*/

        loginPage()

        findViewById<Button>(R.id.button2).setOnClickListener {
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setLogo(R.drawable.map)
                    .setTheme(R.style.LoginTheme)
                    .build(), AUTH_REQUEST_CODE
            )
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTH_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!")
            } else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    private fun loginPage() {
        providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { p0 ->
            val user = p0.currentUser
            if (user != null) {
                /*Toast.makeText(this@AuthenticationActivity ,
                    "Welcome ${FirebaseAuth.getInstance().currentUser?.displayName}" ,
                    Toast.LENGTH_SHORT).show()*/
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
                finish()
            }/*else {
                Toast.makeText(this@AuthenticationActivity , "Please sign in first" , Toast.LENGTH_SHORT).show()
            }*/
        }
    }
}
