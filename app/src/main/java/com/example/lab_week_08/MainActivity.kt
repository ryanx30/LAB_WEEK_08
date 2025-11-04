package com.example.lab_week_08

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker

class MainActivity : AppCompatActivity() {

    // Create an instance of WorkManager
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle edge insets (status bar, nav bar, dll)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize WorkManager instance
        workManager = WorkManager.getInstance(this)

        // Create a constraint: worker hanya jalan kalau ada koneksi internet
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // Request untuk FirstWorker (jalan sekali)
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // Request untuk SecondWorker (jalan setelah FirstWorker selesai)
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // Jalankan worker secara berurutan (First → Second)
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // Observasi hasil dari FirstWorker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("First process is done ✅")
                }
            }

        // Observasi hasil dari SecondWorker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done 🎉")
                }
            }
    }

    // Build input data untuk dikirim ke worker
    private fun getIdInputData(idKey: String, idValue: String): Data =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // Tampilkan hasil ke user (toast)
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
