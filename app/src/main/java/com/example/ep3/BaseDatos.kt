package com.example.ep3

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class BaseDatos(context: Context, nombre: String = "tustudio.db") : SQLiteOpenHelper(context, nombre, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "usuario TEXT UNIQUE, " +
                "contrasena TEXT, " +
                "nivel TEXT, " +
                "correo TEXT)"
        )
        db.execSQL(
            "CREATE TABLE favoritos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "tipo TEXT, " +
                "contenido TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favoritos (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT, contenido TEXT)")
        }
    }
    fun registrar(usuario: String, contrasena: String, nivel: String, correo: String): Boolean {
        val datos = ContentValues()
        datos.put("usuario", usuario)
        datos.put("contrasena", contrasena)
        datos.put("nivel", nivel)
        datos.put("correo", correo)

        val resultado = writableDatabase.insert("usuarios", null, datos)
        return resultado != -1L
    }
    fun iniciarSesion(usuario: String, contrasena: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM usuarios WHERE usuario = ? AND contrasena = ?",
            arrayOf(usuario, contrasena)
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }
    fun guardarFavorito(tipo: String, contenido: String): Boolean {
        if (tipo.isBlank() || contenido.isBlank()) return false
        val datos = ContentValues()
        datos.put("tipo", tipo)
        datos.put("contenido", contenido)

        val resultado = writableDatabase.insert("favoritos", null, datos)
        return resultado != -1L
    }
}

