package com.example.ep3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.database.sqlite.SQLiteException
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)
        val txtCrearCuenta = findViewById<TextView>(R.id.txtCrearCuenta)

        btnIniciar.setOnClickListener {
            val usuario = etUsuario.text.toString()
            val contrasena = etContrasena.text.toString()

            btnIniciar.isEnabled = false
            lifecycleScope.launch {
                try {
                    val valido = withContext(Dispatchers.IO) {
                        BaseDatos(applicationContext).use { it.iniciarSesion(usuario, contrasena) }
                    }

                    if (valido) {
                        val intent = Intent(this@MainActivity, InicioActivity::class.java)
                        intent.putExtra("usuario", usuario)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: SQLiteException) {
                    Toast.makeText(this@MainActivity, "No se pudo abrir la base de datos", Toast.LENGTH_LONG).show()
                } finally { if (!isDestroyed) btnIniciar.isEnabled = true }
            }
        }

        txtCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }
}

