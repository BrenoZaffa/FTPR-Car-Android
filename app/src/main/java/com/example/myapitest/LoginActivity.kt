package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var auth: FirebaseAuth
    private var verificationId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        setupView()
    }

    override fun onResume() {
        super.onResume()
        verifyLoggedUser()
    }

    private fun verifyLoggedUser() {
        if (auth.currentUser != null) {
            navigateToMainActivity()
        }
    }

    private fun setupView() {
        binding.btnSendSms.setOnClickListener {
            sendVerificationCode()
        }
        binding.btnVerifySms.setOnClickListener {
            verifyCode()
        }
    }

    private fun sendVerificationCode() {
        val phoneNumber = binding.cellphone.text.toString()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(45L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential){ }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    //super.onCodeSent(verificationId, token)
                    this@LoginActivity.verificationId = verificationId
                    binding.veryfyCode.visibility = View.VISIBLE
                    binding.btnVerifySms.visibility = View.VISIBLE
                    Toast.makeText(
                        this@LoginActivity,
                        "Código de verificação enviado via SMS",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = binding.veryfyCode.text.toString()
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { onCredentialCompleteListener(it) }
    }

    private fun onCredentialCompleteListener(task: Task<AuthResult>) {
        if (task.isSuccessful) {
            navigateToMainActivity()
        } else {
            Toast.makeText(
                this,
                "${task.exception?.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun navigateToMainActivity() {
        startActivity(MainActivity.newIntent(this))
        finish()
    }

    companion object {

        fun newIntent(context: Context) =
            Intent(context, LoginActivity::class.java)
    }
}