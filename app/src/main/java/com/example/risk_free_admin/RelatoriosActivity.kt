package com.example.risk_free_admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class RelatoriosActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorios)

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Obtenha o SupportMapFragment e seja notificado quando o mapa estiver pronto
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        // Carrega os pontos de risco do Firestore
        carregarPontosDeRisco()
    }

    private fun carregarPontosDeRisco() {
        db.collection("pontos_de_risco")
            .get()
            .addOnSuccessListener { documents ->
                val boundsBuilder = LatLngBounds.builder()

                for (document in documents) {
                    try {
                        val geoPoint = document.getGeoPoint("localizacao") ?: continue
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        val titulo = document.getString("tipo_risco") ?: "Ponto de Risco"
                        val descricao = document.getString("descricao") ?: ""

                        // Adiciona marcador no mapa
                        mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(titulo)
                                .snippet(descricao)
                        )

                        boundsBuilder.include(latLng)
                    } catch (e: Exception) {
                        Log.e("RelatoriosActivity", "Erro ao processar documento ${document.id}", e)
                    }
                }

                // Ajusta a visualização para mostrar todos os marcadores
                try {
                    val bounds = boundsBuilder.build()
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            100,  // padding
                            resources.displayMetrics.widthPixels,
                            resources.displayMetrics.heightPixels,
                            50   // offset
                        )
                    )
                } catch (e: Exception) {
                    Log.e("RelatoriosActivity", "Nenhum ponto para mostrar", e)
                    Toast.makeText(this, "Nenhum ponto de risco encontrado", Toast.LENGTH_SHORT).show()
                }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.w("RelatoriosActivity", "Erro ao carregar pontos", exception)
                Toast.makeText(this, "Falha ao carregar pontos de risco", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    companion object {
        // Classe de modelo para os pontos de risco
        data class PontoRisco(
            val tipo_risco: String = "",
            val descricao: String = "",
            val localizacao: GeoPoint = GeoPoint(0.0, 0.0),
            val data_registro: String = ""
        )
    }
}