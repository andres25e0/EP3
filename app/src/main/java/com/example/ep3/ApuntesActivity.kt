package com.example.ep3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ep3.modelo.RespuestaIA
import com.example.ep3.network.OpenAIAPI
import kotlinx.coroutines.launch
import com.example.ep3.network.ResultadoIA
import androidx.appcompat.app.AlertDialog
class ApuntesActivity : AppCompatActivity() {

    private lateinit var etApunte: EditText
    private lateinit var btnResumen: Button
    private lateinit var btnPreguntas: Button
    private lateinit var btnConceptos: Button
    private lateinit var progressBar: ProgressBar
    
    private lateinit var api: OpenAIAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apuntes)
        val apiKey = BuildConfig.OPENAI_API_KEY
        api = OpenAIAPI(apiKey)

        etApunte = findViewById(R.id.etApunte)
        btnResumen = findViewById(R.id.btnResumen)
        btnPreguntas = findViewById(R.id.btnPreguntas)
        btnConceptos = findViewById(R.id.btnConceptos)
        progressBar = findViewById(R.id.progressBar)
        btnResumen.setOnClickListener {
            ejecutarConsulta("Genera un resumen corto del siguiente tema:")
        }
        btnPreguntas.setOnClickListener {
            ejecutarConsulta("Genera 3 preguntas de estudio (con respuestas) sobre el siguiente tema:")
        }
        btnConceptos.setOnClickListener {
            ejecutarConsulta("Extrae 3 conceptos clave y defínelos, basados en el siguiente tema:")
        }
    }

    private fun ejecutarConsulta(tipoPrompt: String) {
        val textoOriginal = etApunte.text.toString().trim()
        if (textoOriginal.isBlank()) {
            Toast.makeText(this, "Ingresa un texto o tema primero", Toast.LENGTH_SHORT).show()
            return
        }

        val promptCompleto = "$tipoPrompt\n\n$textoOriginal"
        
        mostrarCargando(true)
        lifecycleScope.launch {
            try {
                when (val resultado = api.consultar(promptCompleto)) {
                    is ResultadoIA.Exito -> if (!isFinishing && !isDestroyed) {
                        abrirDetalles(tipoPrompt, textoOriginal, resultado.texto)
                    }
                    is ResultadoIA.Error -> if (!isFinishing && !isDestroyed) {
                        AlertDialog.Builder(this@ApuntesActivity)
                            .setTitle("No se pudo generar la respuesta")
                            .setMessage(resultado.mensaje).setPositiveButton("Aceptar", null).show()
                    }
                }
            } finally {
                if (!isFinishing && !isDestroyed) mostrarCargando(false)
            }
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        btnResumen.isEnabled = !mostrar
        btnPreguntas.isEnabled = !mostrar
        btnConceptos.isEnabled = !mostrar
    }
    private fun abrirDetalles(tipo: String, original: String, resultado: String) {
        val respuestaObjeto = RespuestaIA(
            tipoConsulta = tipo,
            textoOriginal = original,
            resultadoGenerado = resultado
        )

        val intent = Intent(this, DetalleApunteActivity::class.java)
        intent.putExtra("DATOS_API", respuestaObjeto)
        startActivity(intent)
    }
}

