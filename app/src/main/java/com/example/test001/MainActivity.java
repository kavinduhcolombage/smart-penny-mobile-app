package com.example.test001;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    InputStream inputStream;


    private ProgressBar progressBar;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_MICROPHONE_PERMISSION = 1;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;
    OutputStream outputStream;

    TextView statusTextView, voiceCommandTextView, redPenTextView, greenPenTextView, bluePenTextView;
    SpeechRecognizer speechRecognizer;

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        statusTextView.setText("Bluetooth OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        statusTextView.setText("Bluetooth ON");
                        break;
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button btnONOFF = findViewById(R.id.btnONOFF);
        Button btnConnect = findViewById(R.id.btnConnect);
//        Button btnLEDOn = findViewById(R.id.btnLEDOn);
//        Button btnLEDOff = findViewById(R.id.btnLEDOff);
        Button btnVoiceCommand = findViewById(R.id.btnVoiceCommand);
        progressBar = findViewById(R.id.progressBar);

//        redPenTextView = findViewById(R.id.redPenTextView);
//        greenPenTextView = findViewById(R.id.greenPenTextView);
//        bluePenTextView = findViewById(R.id.bluePenTextView);
        statusTextView = findViewById(R.id.statusTextView);



        voiceCommandTextView = findViewById(R.id.voiceCommandTextView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check initial Bluetooth status and update UI
        if (mBluetoothAdapter == null) {
            statusTextView.setText("Bluetooth not supported");
        } else if (mBluetoothAdapter.isEnabled()) {
            statusTextView.setText("Bluetooth ON");
        } else {
            statusTextView.setText("Bluetooth OFF");
        }

        btnONOFF.setOnClickListener(v -> enableDisableBT());
        btnConnect.setOnClickListener(v -> connectToESP32());
//        btnLEDOn.setOnClickListener(v -> sendDataToESP32("ON"));
//        btnLEDOff.setOnClickListener(v -> sendDataToESP32("OFF"));
        btnVoiceCommand.setOnClickListener(v -> startVoiceRecognition());

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE_PERMISSION);
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
    }

    private void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            registerReceiver(mBroadcastReceiver1, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        } else {
            mBluetoothAdapter.disable();
        }
    }

    private void connectToESP32() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("ESP32_BT")) { // Change to your ESP32's Bluetooth name
                mBluetoothDevice = device;
                break;
            }
        }

        if (mBluetoothDevice != null) {
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                mBluetoothSocket.connect();
                outputStream = mBluetoothSocket.getOutputStream();
                inputStream = mBluetoothSocket.getInputStream();  // Initialize input stream
                Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show();
                startListeningForData();  // Start a thread to listen for data
            } catch (IOException e) {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error: " + e.getMessage());
            }
        } else {
            Toast.makeText(this, "ESP32 not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendDataToESP32(String command) {
        try {
            if (outputStream != null) {
                outputStream.write(command.getBytes());
                Toast.makeText(this, "Sent: " + command, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceRecognition() {
        voiceCommandTextView.setText("Listening...");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...");
        // Show the ProgressBar and start its animation
        progressBar.setVisibility(View.VISIBLE);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    String command = matches.get(0);
                    voiceCommandTextView.setText(command);
                    // Check the command and send it to ESP32 via Bluetooth
                    if (command.contains("on")) {
                        sendDataToESP32("ON");  // Send "ON" command
                    } else if (command.contains("off")) {
                        sendDataToESP32("OFF"); // Send "OFF" command
                    } else if (command.contains("blue pen")) {
                        sendDataToESP32("BLUE PEN"); // Send "OFF" command
                    } else if (command.contains("red pen")) {
                        sendDataToESP32("RED PEN"); // Send "OFF" command
                    } else if (command.contains("green pen")) {
                        sendDataToESP32("GREEN PEN"); // Send "OFF" command
                    } else {
                        Toast.makeText(MainActivity.this, "Not valid command for esp 32", Toast.LENGTH_SHORT).show();
                    }
                    // Hide the ProgressBar after the task is complete
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override public void onError(int error) {
                voiceCommandTextView.setText("Error recognizing voice");
                // Hide the ProgressBar
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onEndOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void startListeningForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    if (inputStream != null) {
                        bytes = inputStream.read(buffer);
                        String receivedData = new String(buffer, 0, bytes);

                        runOnUiThread(() -> {
                            //Toast.makeText(MainActivity.this, "Received: " + receivedData, Toast.LENGTH_SHORT).show();
                            if (receivedData.contains("red pen insert")) {
                                //Log.i("TAG", "hello if condition triggered");  // Info message
                                redPenTextView.setText("RED PEN INSERTED");
                            } else if (receivedData.contains("red pen removed")) {
                                redPenTextView.setText("RED PEN REMOVED");
                            }
                            else if (receivedData.contains("blue pen insert")) {
                                bluePenTextView.setText("BLUE PEN INSERTED");
                            } else if (receivedData.contains("blue pen removed")) {
                                bluePenTextView.setText("BLUE PEN REMOVED");
                            }
                            else if (receivedData.contains("green pen insert")) {
                                greenPenTextView.setText("GREEN PEN INSERTED");
                            } else if (receivedData.contains("green pen removed")) {
                                greenPenTextView.setText("GREEN PEN REMOVED");
                            }
                        });
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }


}
