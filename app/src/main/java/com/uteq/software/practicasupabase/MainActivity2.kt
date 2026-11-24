package com.uteq.software.practicasupabase

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.uteq.software.practicasupabase.data.SupabaseManager
import com.uteq.software.practicasupabase.models.Materia
import com.uteq.software.practicasupabase.utils.SupabaseErrorHandler
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity2 : AppCompatActivity() {
    private lateinit var listaNiveles: AutoCompleteTextView
    private lateinit var actvMaterias: AutoCompleteTextView
    private var consultaMaterias: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listaNiveles = findViewById(R.id.listaNiveles)
        actvMaterias = findViewById(R.id.actvmaterias)

        configurarNiveles()
    }

    private fun configurarNiveles() {
        val niveles = resources.getStringArray(R.array.niveles)
        listaNiveles.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, niveles)
        )
        listaNiveles.setOnItemClickListener { parent, _, position, _ ->
            val seleccionado = parent.getItemAtPosition(position).toString()
            Toast.makeText(this, "Seleccionaste: $seleccionado", Toast.LENGTH_SHORT).show()

            // position empieza en 0, por eso el nivel 1 es position + 1
            cargarMaterias(position + 1)
        }
    }

    private fun cargarMaterias(nivel: Int) {
        // Si se cambia de semestre rápido, se descarta la consulta anterior
        consultaMaterias?.cancel()
        actvMaterias.setText("", false)
        actvMaterias.setAdapter(null)

        consultaMaterias = lifecycleScope.launch {
            try {
                val materias = SupabaseManager.client
                    .from("materias_copia")
                    .select {
                        filter { eq("nivel", nivel) }
                        order("nombre", Order.ASCENDING)
                    }
                    .decodeList<Materia>()

                actvMaterias.setAdapter(
                    ArrayAdapter(
                        this@MainActivity2,
                        android.R.layout.simple_dropdown_item_1line,
                        materias.map { it.nombre ?: "" }
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SupabaseErrorHandler.show(this@MainActivity2, e)
            }
        }
    }
}
