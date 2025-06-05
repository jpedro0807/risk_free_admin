package com.example.risk_free_admin

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

        // Inicializa a barra de progresso
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Obtém o fragmento do mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configurações do mapa
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
            isCompassEnabled = true
        }

        // Carrega as ameaças do Firestore
        carregarAmeacasDoFirestore()
    }

    private fun carregarAmeacasDoFirestore() {
        db.collection("ameaca")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhuma ameaça encontrada.", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val boundsBuilder = LatLngBounds.builder()
                var marcadoresAdicionados = 0

                for (document in documents) {
                    try {
                        // Extrai os dados do documento
                        val localizacaoMap = document.get("localizacao") as? Map<*, *>
                        val latitude = localizacaoMap?.get("latitude") as? Double
                        val longitude = localizacaoMap?.get("longitude") as? Double

                        if (latitude == null || longitude == null) {
                            Log.d("RelatoriosActivity", "Documento sem coordenadas válidas: ${document.id}")
                            continue
                        }

                        val latLng = LatLng(latitude, longitude)
                        val descricao = document.getString("descricao") ?: ""
                        val data = document.getString("data") ?: ""
                        val nivel = document.getString("nivel") ?: "0"

                        // Cria o texto para o marcador
                        val titulo = "Ameaça Nível $nivel"
                        val snippet = """
                            ${descricao.take(30)}${if (descricao.length > 30) "..." else ""}
                            Data: $data
                        """.trimIndent()

                        // Adiciona o marcador no mapa
                        mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(titulo)
                                .snippet(snippet)
                        )?.tag = document.id

                        boundsBuilder.include(latLng)
                        marcadoresAdicionados++
                    } catch (e: Exception) {
                        Log.e("RelatoriosActivity", "Erro ao processar documento ${document.id}", e)
                    }
                }

                // Ajusta a visualização do mapa
                if (marcadoresAdicionados > 0) {
                    try {
                        val bounds = boundsBuilder.build()
                        if (marcadoresAdicionados == 1) {
                            // Se houver apenas um marcador, usa zoom
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 14f))
                        } else {
                            // Para múltiplos marcadores, ajusta a visualização
                            mMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    bounds,
                                    resources.displayMetrics.widthPixels,
                                    resources.displayMetrics.heightPixels,
                                    100
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("RelatoriosActivity", "Erro ao ajustar câmera", e)
                    }
                } else {
                    Toast.makeText(this, "Nenhuma ameaça com localização válida", Toast.LENGTH_SHORT).show()
                }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar ameaças: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                Log.e("RelatoriosActivity", "Erro ao carregar ameaças", exception)
            }

        // Configura o clique nos marcadores
        mMap.setOnMarkerClickListener { marker ->
            Toast.makeText(this, marker.title, Toast.LENGTH_SHORT).show()
            true
        }
    }
}