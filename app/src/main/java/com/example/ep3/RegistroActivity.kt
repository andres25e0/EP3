package com.example.ep3

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.database.sqlite.SQLiteException
class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val etNivel = findViewById<EditText>(R.id.etNivel)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

        btnCrear.setOnClickListener {
            val usuario = etUsuario.text.toString()
            val contrasena = etContrasena.text.toString()
            val nivel = etNivel.text.toString()
            val correo = etCorreo.text.toString()

            if (usuario.isEmpty() || contrasena.isEmpty() || nivel.isEmpty() || correo.isEmpty()) {
                Toast.makeText(this@RegistroActivity, "Llena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                btnCrear.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val creado = withContext(Dispatchers.IO) {
                            BaseDatos(applicationContext).use { it.registrar(usuario, contrasena, nivel, correo) }
                        }

                        if (creado) {
                            Toast.makeText(this@RegistroActivity, "Cuenta creada", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@RegistroActivity, "No se pudo crear la cuenta; comprueba si el usuario ya existe", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: SQLiteException) {
                        Toast.makeText(this@RegistroActivity, "No se pudo abrir la base de datos", Toast.LENGTH_LONG).show()
                    } finally { if (!isDestroyed) btnCrear.isEnabled = true }
                }
            }
        }
    }
}

