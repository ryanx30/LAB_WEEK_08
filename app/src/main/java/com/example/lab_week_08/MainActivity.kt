package com.example.lab_week_08

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        workManager = WorkManager.getInstance(this)

        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // Chain: First → Second → Third
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        // Observe workers in sequence
        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) showResult("First process is done ✅")
        }

        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showResult("Second process is done 🎉")
                launchNotificationService()
            }
        }

        workManager.getWorkInfoByIdLiveData(thirdRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showResult("Third process is done 🏁")
                launchSecondNotificationService()
            }
        }
    }

    private fun getIdInputData(idKey: String, idValue: String): Data =
        Data.Builder().putString(idKey, idValue).build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun launchNotificationService() {
        NotificationService.trackingCompletion.observe(this) { id ->
            showResult("NotificationService Channel ID $id done!")
        }

        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun launchSecondNotificationService() {
        SecondNotificationService.trackingCompletion.observe(this) { id ->
            showResult("SecondNotificationService $id completed! 📨")
        }

        val serviceIntent = Intent(this, SecondNotificationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
