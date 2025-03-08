
package gas.trulala.gobus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import android.view.*
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import java.io.*
import java.io.FileOutputStream
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import androidx.core.content.FileProvider

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_OPEN_DOCUMENT = 1
    private val REQUEST_CODE_OPEN_DOCUMENT_TREE = 2
    private var fileYangDipindahkan: File? = null
    private var isPotong = false
    private var direktoriSaatIni: File? = null
    private var proses: Process? = null
    private var output: BufferedReader? = null
    private var input: OutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi direktori saat ini
        direktoriSaatIni = File(filesDir, "home")
        if (!direktoriSaatIni!!.exists()) {
            direktoriSaatIni!!.mkdir()
        }

        val grid = findViewById<GridLayout>(R.id.grid)
        tampilkanFileDiGrid(grid, direktoriSaatIni!!)

        findViewById<ImageView>(R.id.nav).setOnClickListener {
            val liner = findViewById<LinearLayout>(R.id.liner)
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        findViewById<TextView>(R.id.run).setOnClickListener {
            MulaiTerminal()
            findViewById<EditText>(R.id.perintah).setText("")
        }

        findViewById<ImageView>(R.id.tfFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)
        }

        findViewById<ImageView>(R.id.tfFolder).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE)
        }

        findViewById<ImageView>(R.id.fileBaru).setOnClickListener {
            tampilkanDialogKetik()
        }

        findViewById<ImageView>(R.id.folderBaru).setOnClickListener {
            buatFolderBaru(direktoriSaatIni!!, grid)
        }

        findViewById<ImageView>(R.id.kembali).setOnClickListener {
            kembaliKeDirektoriSebelumnya(grid)
        }
    }

    private fun tampilkanFileDiGrid(grid: GridLayout, direktori: File) {
        grid.removeAllViews()

        direktori.listFiles()?.forEach { file ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_vertical, grid, false)
            val gambar = item.findViewById<ImageView>(R.id.gambar)
            val nama = item.findViewById<TextView>(R.id.nama)

            if (file.isFile) {
                nama.text = file.name
                gambar.setImageResource(R.drawable.file)
            } else {
                nama.text = file.name
                gambar.setImageResource(R.drawable.folder)
            }

            grid.addView(item)

            item.setOnClickListener {
                if (file.isFile) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                    intent.setDataAndType(uri, getMimeType(file))
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                } else {
                    MasukFolder(grid, file, findViewById(R.id.kembali))
                }
            }

            item.setOnLongClickListener {
                val lama = findViewById<LinearLayout>(R.id.lama)
                lama.visibility = View.VISIBLE

                val potong = findViewById<ImageView>(R.id.potong)
                val salin = findViewById<ImageView>(R.id.salin)
                val hapus = findViewById<ImageView>(R.id.hapus)
                val rename = findViewById<ImageView>(R.id.rename)
                val tempel = findViewById<ImageView>(R.id.tempel)

                potong.setOnClickListener {
                    Toast.makeText(this, "Memotong ${file.name}", Toast.LENGTH_SHORT).show()
                    fileYangDipindahkan = file
                    isPotong = true
                    tempel.visibility = View.VISIBLE
                }

                salin.setOnClickListener {
                    Toast.makeText(this, "Menyalin ${file.name}", Toast.LENGTH_SHORT).show()
                    fileYangDipindahkan = file
                    isPotong = false
                    tempel.visibility = View.VISIBLE
                }

                hapus.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Hapus File")
                        .setMessage("Apakah Anda yakin ingin menghapus ${file.name}?")
                        .setPositiveButton("Ya") { _, _ ->
                            if (file.delete()) {
                                Toast.makeText(this, "${file.name} dihapus", Toast.LENGTH_SHORT).show()
                                tampilkanFileDiGrid(grid, direktori)
                            } else {
                                Toast.makeText(this, "Gagal menghapus ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }

                rename.setOnClickListener {
                    tampilkanDialogRename(file, grid, direktori)
                    lama.visibility = View.GONE
                }

                tempel.setOnClickListener {
                    if (fileYangDipindahkan != null) {
                        if (isPotong) {
                            val fileTujuan = File(direktori, fileYangDipindahkan!!.name)
                            if (fileYangDipindahkan!!.renameTo(fileTujuan)) {
                                Toast.makeText(this, "File berhasil dipindahkan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Gagal memindahkan file", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val fileTujuan = File(direktori, fileYangDipindahkan!!.name)
                            try {
                                fileYangDipindahkan!!.copyTo(fileTujuan)
                                Toast.makeText(this, "File berhasil disalin", Toast.LENGTH_SHORT).show()
                            } catch (e: IOException) {
                                Toast.makeText(this, "Gagal menyalin file", Toast.LENGTH_SHORT).show()
                            }
                        }
                        tampilkanFileDiGrid(grid, direktori)
                        tempel.visibility = View.GONE
                    } else {
                        Toast.makeText(this, "Tidak ada file yang dipilih", Toast.LENGTH_SHORT).show()
                    }
                }

                true
            }
        }
    }

    private fun MasukFolder(grid: GridLayout, folder: File, kembali: ImageView) {
        if (folder.isDirectory) {
            direktoriSaatIni = folder
            tampilkanFileDiGrid(grid, folder)
            kembali.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Ini adalah file, bukan folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun kembaliKeDirektoriSebelumnya(grid: GridLayout) {
        val direktoriInduk = direktoriSaatIni?.parentFile
        if (direktoriInduk != null) {
            direktoriSaatIni = direktoriInduk
            tampilkanFileDiGrid(grid, direktoriInduk)
        } else {
            Toast.makeText(this, "Anda sudah di direktori root", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tampilkanDialogRename(file: File, grid: GridLayout, direktori: File) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.item_ketik, null)
        val tulis = dialogView.findViewById<EditText>(R.id.tulis)
        val batal = dialogView.findViewById<Button>(R.id.batal)
        val mulai = dialogView.findViewById<Button>(R.id.mulai)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Ganti Nama")
            .create()

        batal.setOnClickListener {
            dialog.dismiss()
        }

        mulai.setOnClickListener {
            val namaBaru = tulis.text.toString()
            if (namaBaru.isNotEmpty()) {
                val fileBaru = File(file.parent, namaBaru)
                if (file.renameTo(fileBaru)) {
                    Toast.makeText(this, "File berhasil diubah nama", Toast.LENGTH_SHORT).show()
                    tampilkanFileDiGrid(grid, direktori)
                } else {
                    Toast.makeText(this, "Gagal mengubah nama file", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun tampilkanDialogKetik() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.item_ketik, null)
        val tulis = dialogView.findViewById<EditText>(R.id.tulis)
        val batal = dialogView.findViewById<Button>(R.id.batal)
        val mulai = dialogView.findViewById<Button>(R.id.mulai)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Buat File Baru")
            .create()

        batal.setOnClickListener {
            dialog.dismiss()
        }

        mulai.setOnClickListener {
            val namaFile = tulis.text.toString()
            if (namaFile.isNotEmpty()) {
                val fileBaru = File(direktoriSaatIni, namaFile)
                if (fileBaru.createNewFile()) {
                    tampilkanFileDiGrid(findViewById(R.id.grid), direktoriSaatIni!!)
                    Toast.makeText(this, "File berhasil dibuat", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal membuat file", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Nama file tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun buatFolderBaru(direktori: File, grid: GridLayout) {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Buat Folder Baru")
            .setMessage("Masukkan nama folder:")
            .setView(input)
            .setPositiveButton("Buat") { _, _ ->
                val namaFolder = input.text.toString()
                if (namaFolder.isNotEmpty()) {
                    val folderBaru = File(direktori, namaFolder)
                    if (folderBaru.mkdir()) {
                        tampilkanFileDiGrid(grid, direktori)
                        Toast.makeText(this, "Folder berhasil dibuat", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal membuat folder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val uri = data.data
            when (requestCode) {
                REQUEST_CODE_OPEN_DOCUMENT -> {
                    uri?.let { copyFileToInternalStorage(it, direktoriSaatIni!!) }
                }
                REQUEST_CODE_OPEN_DOCUMENT_TREE -> {
                    uri?.let { copyDirectoryToInternalStorage(it, direktoriSaatIni!!) }
                }
            }
        }
    }

    private fun copyFileToInternalStorage(uri: Uri, destinationDir: File) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)
            val outputStream = FileOutputStream(File(destinationDir, fileName))

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            tampilkanFileDiGrid(findViewById(R.id.grid), destinationDir)
            Toast.makeText(this, "File berhasil disalin ke ${destinationDir.path}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyalin file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyDirectoryToInternalStorage(uri: Uri, destinationDir: File) {
        try {
            val documentTree = DocumentFile.fromTreeUri(this, uri)
            documentTree?.listFiles()?.iterator()?.forEach { documentFile ->
                val fileName = documentFile.name
                val inputStream = contentResolver.openInputStream(documentFile.uri)
                val outputStream = FileOutputStream(File(destinationDir, fileName))

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            }

            tampilkanFileDiGrid(findViewById(R.id.grid), destinationDir)
            Toast.makeText(this, "Direktori berhasil disalin ke ${destinationDir.path}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyalin direktori", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown"
    }

    private fun getMimeType(file: File): String {
        return when (file.extension) {
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
    }

private fun MulaiTerminal() {
    val perintah = findViewById<EditText>(R.id.perintah).text.toString()
    val hasil = findViewById<TextView>(R.id.hasil)
    
    try {
        if (proses == null) {
            val binPath = "/data/data/myapp/files/usr/bin/sh"
            proses = ProcessBuilder(binPath).redirectErrorStream(true).start()
            output = BufferedReader(InputStreamReader(proses!!.inputStream))
            input = proses!!.outputStream
        }

        input?.write("$perintah\n".toByteArray())
        input?.flush()

        var line: String?
        while (output?.readLine().also { line = it } != null) {
            hasil.append(line + "\n")
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
}
}
