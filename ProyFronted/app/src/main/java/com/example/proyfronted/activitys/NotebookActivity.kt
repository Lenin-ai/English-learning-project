package com.example.proyfronted.activitys

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyfronted.backend.Notes.Client.RetrofitClient
import com.example.proyfronted.backend.Notes.dto.NoteBookDto
import com.example.proyfronted.R
import com.example.proyfronted.ui.adapters.Notes.NotebookAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotebookActivity : AppCompatActivity() {

    private lateinit var recyclerNotebooks: RecyclerView
    private lateinit var btnAddNotebook: Button
    private lateinit var btnBackToHome: Button
    private val notebooks = mutableListOf<NoteBookDto>()
    private lateinit var notebookAdapter: NotebookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notebook)

        recyclerNotebooks = findViewById(R.id.recyclerNotebooks)
        btnAddNotebook = findViewById(R.id.btnAddNotebook)
        btnBackToHome = findViewById(R.id.btnBackToHome)

        notebookAdapter = NotebookAdapter(
            notebooks,
            onClick = { openNotesActivity(it) },
            onDelete = { notebook, position -> confirmDeleteNotebook(notebook, position) }
        )

        recyclerNotebooks.layoutManager = LinearLayoutManager(this)
        recyclerNotebooks.adapter = notebookAdapter

        btnAddNotebook.setOnClickListener { showCreateNotebookDialog() }
        btnBackToHome.setOnClickListener { finish() }

        loadNotebooks()
    }

    private fun loadNotebooks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = RetrofitClient.noteBookApi.getMyNotebooks()
                withContext(Dispatchers.Main) {
                    notebooks.clear()
                    notebooks.addAll(result)
                    notebookAdapter.notifyDataSetChanged()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotebookActivity, "Error al cargar cuadernos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCreateNotebookDialog() {
        val editText = EditText(this).apply { hint = "Título del cuaderno" }

        AlertDialog.Builder(this)
            .setTitle("Nuevo Cuaderno")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    createNotebook(title)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createNotebook(title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dto = NoteBookDto(title = title)
                val result = RetrofitClient.noteBookApi.createNotebook(dto)

                withContext(Dispatchers.Main) {
                    notebooks.add(0, result)
                    notebookAdapter.notifyItemInserted(0)
                    recyclerNotebooks.scrollToPosition(0)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotebookActivity, "Error al crear cuaderno", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDeleteNotebook(notebook: NoteBookDto, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuaderno")
            .setMessage("¿Estás seguro de eliminar este cuaderno? Se eliminarán también sus notas.")
            .setPositiveButton("Sí") { _, _ ->
                deleteNotebook(notebook.id ?: return@setPositiveButton, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteNotebook(id: Long, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.noteBookApi.deleteNotebook(id)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        notebooks.removeAt(position)
                        notebookAdapter.notifyItemRemoved(position)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NotebookActivity, "Error al eliminar cuaderno", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotebookActivity, "Error al eliminar cuaderno", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun openNotesActivity(notebook: NoteBookDto) {
        val id = notebook.id
        if (id == null) {
            Toast.makeText(this, "Error: cuaderno sin ID", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, NotesActivity::class.java)
        intent.putExtra("NOTEBOOK_ID", id)
        intent.putExtra("NOTEBOOK_TITLE", notebook.title ?: "Sin título")
        startActivity(intent)
    }
}
