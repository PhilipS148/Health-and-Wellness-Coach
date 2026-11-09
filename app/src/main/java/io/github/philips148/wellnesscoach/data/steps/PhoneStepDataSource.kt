package io.github.philips148.wellnesscoach.data.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.data.LocalField
import com.google.android.gms.fitness.request.LocalDataReadRequest
import io.github.philips148.wellnesscoach.data.model.StepTotal
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class PhoneStepDataSource(context: Context) {

    // Keep the application context so this object does not hold
    // onto an Activity after its screen closes.
    private val appContext = context.applicationContext

    // Create the Recording API client when it is first needed.
    private val recordingClient by lazy {
        FitnessLocal.getLocalRecordingClient(appContext)
    }

    // Check whether Android reports a hardware step counter.
    fun hasStepCounter(): Boolean {
        return appContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_SENSOR_STEP_COUNTER
        )
    }

    // Return Google's status code so the screen can distinguish
    // working services from missing, disabled, or outdated services.
    fun getPlayServicesStatus(): Int {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(
                appContext,
                LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE
            )
    }

    // Check permission without showing a permission dialog.
    // The Steps screen will handle requesting permission.
    fun hasActivityPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun readTodayTotal(): StepTotal {
        // Stop before using the API when basic requirements are missing.
        check(hasStepCounter()) {
            "Phone-based step tracking is unavailable on this device."
        }

        check(getPlayServicesStatus() == ConnectionResult.SUCCESS) {
            "Google Play services must be available and up to date."
        }

        // Check permission directly before calling the Recording API.
        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException(
                "Allow physical activity access to record steps."
            )
        }

        // Start background step collection.
        // Calling subscribe again keeps an existing subscription.
        recordingClient.subscribe(
            LocalDataType.TYPE_STEP_COUNT_DELTA
        ).await()

        // Capture the phone's current date and time together.
        // Calculate local midnight rather than subtracting 24 hours,
        // so daylight-saving changes are handled correctly.
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val startMillis = today.atStartOfDay(now.zone)
            .toInstant()
            .toEpochMilli()
        val endMillis = now.toInstant().toEpochMilli()

        // Ask Google to total recorded steps from midnight until now.
        val request = LocalDataReadRequest.Builder()
            .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(
                startMillis,
                endMillis,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Errors propagate to the caller instead of becoming zero steps.
        val response = recordingClient.readData(request).await()

        // Google returns grouped datasets containing step-count values.
        // Combine those values using Long to avoid Int overflow.
        val steps = response.buckets
            .flatMap { bucket -> bucket.dataSets }
            .flatMap { dataSet -> dataSet.dataPoints }
            .sumOf { point ->
                point.getValue(LocalField.FIELD_STEPS)
                    .asInt()
                    .toLong()
            }

        // Our shared StepTotal model stores a non-negative Int.
        check(steps in 0L..Int.MAX_VALUE.toLong()) {
            "The phone returned an invalid step total."
        }

        // Keep the count attached to the date used for this request,
        // even if the request finishes after midnight.
        val date = today.toString()

        return StepTotal(
            id = date,
            date = date,
            steps = steps.toInt()
        )
    }
}