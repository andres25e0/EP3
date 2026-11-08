package com.example.ep3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class InicioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        val usuario = intent.getStringExtra("usuario")
        val txtSaludo = findViewById<TextView>(R.id.txtSaludo)
        val btnSalir = findViewById<Button>(R.id.btnSalir)
        val btnAsistenteIA = findViewById<Button>(R.id.btnAsistenteIA)

        txtSaludo.text = "¡Hola, $usuario!"

        btnAsistenteIA.setOnClickListener {
            startActivity(Intent(this, ApuntesActivity::class.java))
        }

        btnSalir.setOnClickListener {
            finish()
        }
    }
}
