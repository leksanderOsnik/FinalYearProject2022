package com.project.effidrive

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.project.effidrive.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    companion object{
        private const val RC_SIGN_IN = 100
        private const val TAG_GOOGLE_SIGN_IN = "GOOGLE_SIGN_IN_TAG"
        private const val TAG_EMAIL_PASSWORD_SIGN_IN = "EMAIL_PASSWORD_SIGN_IN_TAG"
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // User is signed in
            val i = Intent(this@LoginActivity, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        } else {
            // User is signed out
            Log.d("Login", "onAuthStateChanged:signed_out")
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        auth = FirebaseAuth.getInstance()
        checkUser()
        googleSignInClient.signOut()



        binding.registerRedirect.setOnClickListener{
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }






        binding.buttonSignIn.setOnClickListener{
            when{
                TextUtils.isEmpty(binding.emailUsername.text.toString().trim{it <= ' '}) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter email.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG_EMAIL_PASSWORD_SIGN_IN, "Email field left empty")
                }

                TextUtils.isEmpty(binding.passwordField.text.toString().trim{it <= ' '}) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter password.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG_EMAIL_PASSWORD_SIGN_IN, "Password field left empty")
                }
                else -> {
                    val email: String = binding.emailUsername.text.toString().trim { it <= ' ' }
                    val password: String = binding.passwordField.text.toString().trim { it <= ' ' }
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(
                            OnCompleteListener<AuthResult> { task ->

                                if (task.isSuccessful) {

                                    val firebaseUser: FirebaseUser = task.result!!.user!!
                                    Log.d(TAG_EMAIL_PASSWORD_SIGN_IN, "EmailPasswordAuth: UID: ${auth.uid}")
                                    Log.d(TAG_EMAIL_PASSWORD_SIGN_IN, "EmailPasswordAuth: Email: ${firebaseUser.email}")
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "You have signed in successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent =
                                        Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    intent.putExtra("user_id", firebaseUser.uid)
                                    intent.putExtra("email_id", email)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        task.exception!!.message.toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )



                     }
                 }
        }
    }

    private fun checkUser() {
        binding?.googleSignInBtn.setOnClickListener {
            Log.d(TAG_GOOGLE_SIGN_IN, "onCreate: begin Google SignIn")
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == RC_SIGN_IN){
            Log.d(TAG_GOOGLE_SIGN_IN,"onActivityResult: Google SignIn intent result")
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                val account = accountTask.getResult(ApiException::class.java)
                firebaseAuthWithGoogleAccount(account)
            }
            catch(e: Exception){
                //failed Google SignIn
                Log.d(TAG_GOOGLE_SIGN_IN, "onActivityResult: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogleAccount(account: GoogleSignInAccount?) {
        Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: begin firebase auth with google account")
        val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                //login successful
                Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: LoggedIn")

                val firebaseUser = auth.currentUser

                val uid = auth!!.uid
                val email = firebaseUser!!.email

                Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: UID: $uid")
                Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: Email: $email")

                if(authResult.additionalUserInfo!!.isNewUser){
                    //new user - account created
                    Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: Account created \n$email")
                    Toast.makeText(this@LoginActivity, "Account created... \n$email", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("user_id", firebaseUser.uid)
                    startActivity(intent)
                }
                else{
                    //existing user
                    Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: Existing user...\n$email")
                    Toast.makeText(this@LoginActivity, "Logged in with  \n$email", Toast.LENGTH_SHORT).show()
                }

                val intent =
                    Intent(this@LoginActivity, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("user_id", firebaseUser.uid)
                intent.putExtra("email_id", email)
                startActivity(intent)
                finish()



            }
            .addOnFailureListener{ e ->
                Log.d(TAG_GOOGLE_SIGN_IN, "firebaseAuthWithGoogleAccount: Login Failed due to ${e.message}")
                Toast.makeText(this@LoginActivity, "Login Failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    }

