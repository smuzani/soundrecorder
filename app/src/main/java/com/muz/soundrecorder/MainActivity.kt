package com.muz.soundrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    var filename: String = ""
    val mediaRecorder = MediaRecorder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var intent = Intent()
        intent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(intent, TTS_CODE)
    }

    val SPEECH_TEXT = 100
    fun record() {
        text.text = "Recording..."
        val pmanager = this.packageManager
//        var intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        intent.putExtra(
//            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//        )
//        startActivityForResult(intent, SPEECH_TEXT)
        if (pmanager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            filename = Environment.getExternalStorageDirectory().getAbsolutePath()
            filename += "/audio.3gp"
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder.setOutputFile(filename)
            try {
                mediaRecorder.prepare()
                mediaRecorder.start()
            } catch (e: IOException) {
                Log.w("startRecording failed", e)
            }
        } else {
            Toast.makeText(this, "This device doesn't have a microphone", Toast.LENGTH_LONG).show()
        }
    }

    fun openSpeechRecognition(v: View) {
        var intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        startActivityForResult(intent, SPEECH_TEXT)
    }


    val MIC_REQUEST = 123
    fun startRecording(v: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), MIC_REQUEST
                )
            } else {
                record()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        if (requestCode == MIC_REQUEST) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                record()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun stopRecording(v: View) {
        text.text = ""
        mediaRecorder.stop()
        mediaRecorder.reset()
        mediaRecorder.release()

        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(filename)
        mediaPlayer.prepare()
        mediaPlayer.start()
    }

    val TTS_CODE = 141

    var tts: TextToSpeech? = null
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Toast.makeText(applicationContext, "Success initializing TTS", Toast.LENGTH_SHORT)
                .show()
        } else if (status == TextToSpeech.ERROR) {
            Toast.makeText(applicationContext, "Failed to initialize", Toast.LENGTH_SHORT).show()
        }
    }

    var recordedResults = ""
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_TEXT && resultCode == Activity.RESULT_OK) {
            var matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            recordedResults = ""
            matches?.forEach {
                recordedResults += "$it\n\n"
            }
            text.text = recordedResults
        }
        if (requestCode == TTS_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = TextToSpeech(this, this)
            } else {
                val installTTSIntent = Intent()
                installTTSIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installTTSIntent)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun startTts(v: View) {
        tts?.language = Locale.US
        tts?.speak(recordedResults, TextToSpeech.QUEUE_FLUSH, null)
    }

}
