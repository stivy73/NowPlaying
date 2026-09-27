package com.kieronquinn.app.pixelambientmusic.components.recogniser

import android.os.IBinder
import com.kieronquinn.app.pixelambientmusic.IRecognitionCallback
import com.kieronquinn.app.pixelambientmusic.model.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MusicRecogniserTest {
    private val ids = mutableListOf<String>()

    @Before fun resetState() {
        MusicRecogniser.onRecognitionSkipped(MusicRecogniser.SkippedReason.MUSIC, RecognitionSource.NNFP)
    }

    @After fun removeCallbacks() {
        ids.forEach { MusicRecogniser.removeCallback(it) }
    }

    private fun register(source: RecognitionSource, onFailure: () -> Unit): String {
        val callback = object : IRecognitionCallback {
            override fun asBinder(): IBinder? = null
            override fun onRecordingStarted() = Unit
            override fun onRecognitionStarted() = Unit
            override fun onRecognitionSucceeded(result: RecognitionResult, metadata: RecognitionMetadata?) = Unit
            override fun onRecognitionFailed(result: RecognitionFailure) = onFailure()
        }
        return MusicRecogniser.addCallback(callback, RecognitionCallbackMetadata(source, false))
            .also { ids.add(it) }
    }

    @Test fun skippedOfflineRecognitionTerminatesOfflineButNotOnDemand() {
        var offlineFailures = 0
        var onlineFailures = 0
        register(RecognitionSource.NNFP) { offlineFailures++ }
        register(RecognitionSource.ON_DEMAND) { onlineFailures++ }
        MusicRecogniser.onStartRecording()
        MusicRecogniser.onRecognitionSkipped(MusicRecogniser.SkippedReason.MUSIC, RecognitionSource.NNFP)
        assertEquals(1, offlineFailures)
        assertEquals(0, onlineFailures)
    }

    @Test fun audioFailureTerminatesBothSources() {
        var failures = 0
        register(RecognitionSource.NNFP) { failures++ }
        register(RecognitionSource.ON_DEMAND) { failures++ }
        MusicRecogniser.onRecognitionSkipped(
            MusicRecogniser.SkippedReason.AUDIO_RECORD_FAILED, RecognitionSource.NNFP, true)
        assertEquals(2, failures)
    }

    @Test fun noMusicTerminatesOfflineButNotOnDemand() {
        var offlineFailures = 0
        var onlineFailures = 0
        register(RecognitionSource.NNFP) { offlineFailures++ }
        register(RecognitionSource.ON_DEMAND) { onlineFailures++ }
        MusicRecogniser.onStartRecording()
        MusicRecogniser.onRecognitionFailed(RecognitionFailure(
            RecognitionFailureReason.NoMatch, RecognitionSource.NNFP, null))
        assertEquals(1, offlineFailures)
        assertEquals(0, onlineFailures)
    }

    @Test fun callbacksCanUnregisterDuringDelivery() {
        var failures = 0
        repeat(3) {
            lateinit var id: String
            id = register(RecognitionSource.NNFP) {
                failures++
                MusicRecogniser.removeCallback(id)
            }
        }
        MusicRecogniser.onRecognitionSkipped(MusicRecogniser.SkippedReason.MUSIC, RecognitionSource.NNFP, true)
        assertEquals(3, failures)
    }
}
