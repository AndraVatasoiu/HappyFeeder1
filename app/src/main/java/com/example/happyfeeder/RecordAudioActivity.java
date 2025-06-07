package com.example.happyfeeder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

public class RecordAudioActivity extends AppCompatActivity {

    private static final int REQUEST_MIC_PERMISSION = 200;
    private static final int MAX_DURATION_MS = 20 * 1000; // 20 secunde

    private ImageButton recordButton;
    private Button sendButton;
    private TextView timerText, statusText;

    private MediaRecorder recorder;
    private boolean isRecording = false;
    private String audioFilePath;

    private CountDownTimer countDownTimer;
    private long timeLeft = MAX_DURATION_MS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);

        recordButton = findViewById(R.id.recordButton);
        sendButton = findViewById(R.id.sendButton);
        timerText = findViewById(R.id.timerText);
        statusText = findViewById(R.id.statusText);

        sendButton.setVisibility(View.GONE);

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                checkMicPermissionAndStart();
            }
        });

        sendButton.setOnClickListener(v -> {
            Toast.makeText(this, "Înregistrarea a fost trimisă!", Toast.LENGTH_SHORT).show();
            // Aici poți implementa logica de upload către server sau Firebase
        });
    }

    private void checkMicPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/recorded_audio.3gp";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(audioFilePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            statusText.setText("Înregistrează...");
            startTimer();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Eroare la înregistrare", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            recorder.stop();
            recorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recorder = null;
        isRecording = false;
        timeLeft = MAX_DURATION_MS;

        if (countDownTimer != null) countDownTimer.cancel();

        timerText.setText("00:00");
        statusText.setText("Înregistrare oprită");
        sendButton.setVisibility(View.VISIBLE);
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                int seconds = (int) (timeLeft / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                timerText.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                stopRecording();
            }
        }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Permisiunea pentru microfon este necesară!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
