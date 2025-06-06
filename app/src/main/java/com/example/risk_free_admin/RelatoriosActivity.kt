package com.example.risk_free_admin

import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class RelatoriosActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorios)

        mapView = findViewById(R.id.mapView)
        progressBar = findViewById(R.id.progressBar)

        val btnExportar: ImageButton = findViewById(R.id.btnExportCsv)
        btnExportar.setOnClickListener {
            exportarCSV()
        }

        progressBar.visibility = View.VISIBLE

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            configurarMapa()
            carregarAmeacasDoFirestore()
        }
    }

    private fun configurarMapa() {
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
            isCompassEnabled = true
        }

        googleMap.setOnMarkerClickListener { marker ->
            Toast.makeText(this, marker.title, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun carregarAmeacasDoFirestore() {
        db.collection("ameaca")
            .get()
            .addOnSuccessListener { documents ->
                val boundsBuilder = LatLngBounds.builder()
                var marcadoresAdicionados = 0

                for (document in documents) {
                    val localizacaoMap = document.get("localizacao") as? Map<*, *>
                    val latitude = localizacaoMap?.get("latitude") as? Double
                    val longitude = localizacaoMap?.get("longitude") as? Double

                    if (latitude != null && longitude != null) {
                        val latLng = LatLng(latitude, longitude)
                        val descricao = document.getString("descricao") ?: ""
                        val data = document.getString("data") ?: ""
                        val nivel = document.getString("nivel") ?: "0"

                        val titulo = "Ameaça Nível $nivel"
                        val snippet = """
                            ${descricao.take(30)}${if (descricao.length > 30) "..." else ""}
                            Data: $data
                        """.trimIndent()

                        googleMap.addMarker(
                            MarkerOptions().position(latLng).title(titulo).snippet(snippet)
                        )?.tag = document.id

                        boundsBuilder.include(latLng)
                        marcadoresAdicionados++
                    }
                }

                if (marcadoresAdicionados > 0) {
                    try {
                        val bounds = boundsBuilder.build()
                        if (marcadoresAdicionados == 1) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, 14f))
                        } else {
                            googleMap.animateCamera(
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
                    Toast.makeText(this, "Nenhuma ameaça encontrada com localização válida", Toast.LENGTH_SHORT).show()
                }

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar ameaças: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    private fun exportarCSV() {
        db.collection("ameaca")
            .get()
            .addOnSuccessListener { documents ->
                val csvBuilder = StringBuilder()
                csvBuilder.append("ID,Descrição,Nível,Data,Latitude,Longitude\n")

                for (document in documents) {
                    val id = document.id
                    val descricao = document.getString("descricao") ?: ""
                    val nivel = document.getString("nivel") ?: ""
                    val data = document.getString("data") ?: ""

                    val localizacao = document.get("localizacao") as? Map<*, *>
                    val latitude = localizacao?.get("latitude")?.toString() ?: ""
                    val longitude = localizacao?.get("longitude")?.toString() ?: ""

                    csvBuilder.append("$id,\"$descricao\",$nivel,$data,$latitude,$longitude\n")
                }

                salvarCSV(csvBuilder.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao gerar relatório: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun salvarCSV(conteudo: String) {
        val fileName = "relatorio_ameacas.csv"
        val mimeType = "text/csv"

        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(conteudo.toByteArray())
                Toast.makeText(this, "Relatório salvo na pasta Download", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "Erro ao salvar o arquivo", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
