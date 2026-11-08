package com.example.ep3

import android.os.Bundle
import android.database.sqlite.SQLiteException
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.example.ep3.modelo.RespuestaIA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleApunteActivity : AppCompatActivity() {
    private var guardado = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)
        val tipo = findViewById<TextView>(R.id.tvTipoConsulta)
        val resultado = findViewById<TextView>(R.id.tvResultadoFinal)
        val favorito = findViewById<Button>(R.id.btnFavorito)
        val datos = IntentCompat.getSerializableExtra(intent, "DATOS_API", RespuestaIA::class.java)
        if (datos == null || datos.resultadoGenerado.isBlank() || datos.textoOriginal.isBlank() || datos.tipoConsulta.isBlank()) {
            resultado.text = "No hay una respuesta válida para mostrar."
            favorito.isEnabled = false
            return
        }
        tipo.text = datos.tipoConsulta
        resultado.text = "Tema: ${datos.textoOriginal}\n\n${datos.resultadoGenerado}"
        guardado = savedInstanceState?.getBoolean("guardado") ?: false
        favorito.isEnabled = !guardado
        if (guardado) favorito.text = "¡Guardado!"
        favorito.setOnClickListener {
            favorito.isEnabled = false
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        BaseDatos(applicationContext).use { db ->
                            db.guardarFavorito(datos.tipoConsulta, "Tema: ${datos.textoOriginal}\n\n${datos.resultadoGenerado}")
                        }
                    } catch (_: SQLiteException) { false }
                }
                guardado = ok
                if (!isDestroyed && !isFinishing) {
                    favorito.isEnabled = !ok
                    if (ok) favorito.text = "¡Guardado!"
                    Toast.makeText(this@DetalleApunteActivity,
                        if (ok) "Guardado en Favoritos correctamente" else "No se pudo guardar en SQLite. Intenta de nuevo.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("guardado", guardado)
        super.onSaveInstanceState(outState)
    }
}
