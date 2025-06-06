package com.example.risk_free_admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val btnVerificarAmeacas: Button = findViewById(R.id.btnVerificarAmeacas)

        val btnRelatorios: Button = findViewById(R.id.btnRelatorios)

        val btnNavVerificarAmeacas : ImageButton = findViewById(R.id.btnNavVerificarAmeacas)

        val btnNavExportRelatorio : ImageButton = findViewById((R.id.btnNavExportRelatorio))

        val navegarPraReport = {
            startActivity(Intent(this, VerificarAmeacaActivity::class.java))
        }
        val navegarPraRelatoriosActivity = {
            startActivity(Intent(this, RelatoriosActivity::class.java))
        }

        btnVerificarAmeacas.setOnClickListener { navegarPraReport() }
        btnNavVerificarAmeacas   .setOnClickListener { navegarPraReport() }
        btnRelatorios.setOnClickListener{navegarPraRelatoriosActivity()}
        btnNavExportRelatorio.setOnClickListener{navegarPraRelatoriosActivity()}
    }
}