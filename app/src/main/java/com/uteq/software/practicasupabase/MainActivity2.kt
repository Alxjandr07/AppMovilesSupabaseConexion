package com.uteq.software.practicasupabase

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.uteq.software.practicasupabase.data.SupabaseManager
import com.uteq.software.practicasupabase.models.Alumno
import com.uteq.software.practicasupabase.models.Materia
import com.uteq.software.practicasupabase.utils.SupabaseErrorHandler
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity2 : AppCompatActivity() {
    private var consultaMaterias: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras del sistema claras (íconos oscuros) porque el fondo es blanco
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Los semestres ya vienen del XML con app:simpleItems="@array/niveles"
        val listaNiveles = findViewById<MaterialAutoCompleteTextView>(R.id.listaNiveles)
        listaNiveles.setOnItemClickListener { parent, _, position, _ ->
            val seleccionado = parent.getItemAtPosition(position).toString()
            Toast.makeText(this, "Seleccionaste: $seleccionado", Toast.LENGTH_SHORT).show()

            // position empieza en 0, por eso el nivel 1 es position + 1
            cargarMaterias(position + 1)
        }

        // Encabezado de la lista: se agrega antes de cargar los alumnos
        val lvAlumnos = findViewById<ListView>(R.id.lvAlumnos)
        val header = layoutInflater.inflate(R.layout.ly_header, lvAlumnos, false)
        lvAlumnos.addHeaderView(header, null, false)

        // getItemAtPosition() devuelve el mapa del alumno aunque haya encabezado
        lvAlumnos.setOnItemClickListener { parent, _, position, _ ->
            val alumno = parent.getItemAtPosition(position) as? Map<*, *>
                ?: return@setOnItemClickListener
            Toast.makeText(this, "Item seleccionado: ${alumno["nombres"]}",
                Toast.LENGTH_SHORT).show()
        }

        // Los alumnos se cargan al abrir la pantalla; no dependen del semestre
        cargarAlumnos()
    }

    private fun cargarAlumnos() {
        lifecycleScope.launch {
            try {
                val alumnos = SupabaseManager.client.from("alumnos")
                    .select { order("nombres", Order.ASCENDING) }
                    .decodeList<Alumno>()

                // Cada alumno se vuelve un mapa; sus claves se enlazan con los TextView de item_alumno.xml
                val datos = alumnos.map {
                    mapOf(
                        "id" to it.id,
                        "nombres" to it.nombres.orEmpty(),
                        "correo" to it.correo.orEmpty(),
                        "telefono" to it.telefono.orEmpty(),
                        "foto" to it.foto.orEmpty()
                    )
                }
                val adapter = SimpleAdapter(
                    this@MainActivity2, datos, R.layout.item_alumno,
                    arrayOf("foto", "nombres", "correo", "telefono"),
                    intArrayOf(R.id.imgFoto, R.id.txtNombre, R.id.txtCorreo, R.id.txtTelefono)
                )
                // SimpleAdapter no sabe descargar imágenes de internet: la foto la carga Glide
                adapter.viewBinder = SimpleAdapter.ViewBinder { view, data, _ ->
                    if (view !is ImageView) return@ViewBinder false
                    val gris = ColorDrawable(Color.LTGRAY)
                    Glide.with(view)
                        .load((data as? String)?.ifEmpty { null })
                        .placeholder(gris)
                        .error(gris)
                        .dontAnimate() // CircleImageView no admite la animación de Glide
                        .into(view)
                    true
                }
                findViewById<ListView>(R.id.lvAlumnos).adapter = adapter
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SupabaseErrorHandler.show(this@MainActivity2, e)
            }
        }
    }

    private fun cargarMaterias(nivel: Int) {
        // Si se cambia de semestre rápido, se descarta la consulta anterior
        consultaMaterias?.cancel()

        consultaMaterias = lifecycleScope.launch {
            val selector = findViewById<MaterialAutoCompleteTextView>(R.id.listaMaterias)
            selector.setText("", false)
            selector.setSimpleItems(emptyArray<String>())
            try {
                val materias = SupabaseManager.client.from("materias_copia")
                    .select {
                        filter { eq("nivel", nivel) }
                        order("nombre", Order.ASCENDING)
                    }
                    .decodeList<Materia>()

                // map() saca los nombres y toTypedArray() los vuelve el arreglo que pide setSimpleItems()
                selector.setSimpleItems(
                    materias.map { it.nombre.orEmpty() }.toTypedArray()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SupabaseErrorHandler.show(this@MainActivity2, e)
            }
        }
    }
}
