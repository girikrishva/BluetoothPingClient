package com.snapbizz.bluetoothpingclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice device = null;
    private TextView textView = null;
    private int REQUEST_ENABLE_BT = 1;
    private Button button = null;
    private CheckBox checkBox = null;
    private EditText editText = null;
    private TextView textViewInterval = null;
    private String timeBytesString = null;
    private long counter = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textViewMain);
        button = findViewById(R.id.button);
        button.setX(750);
        button.setY(40);
        button.setOnClickListener(this);
        checkBox = findViewById(R.id.checkBox);
        checkBox.setX(900);
        checkBox.setY(50);
        checkBox.setOnClickListener(this);
        textViewInterval = findViewById(R.id.textViewInterval);
        textViewInterval.setX(1040);
        textViewInterval.setY(56);
        textViewInterval.setText("Interval");
        editText = findViewById(R.id.editText);
        editText.setX(1100);
        editText.setY(40);
        editText.setText("10");
        setBluetoothAdapter();
    }

    private void setBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            enableBluetoothAdapter();
        }
    }

    private void enableBluetoothAdapter() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        setPairedDevice();
    }

    private void setPairedDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) device = (BluetoothDevice) pairedDevices.toArray()[0];
    }

    public void updateTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                textView.setText(timeBytesString);
            }
        });
    }

    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            new GetServerTimeThread(bluetoothAdapter, device, false).start();
        } else if (v.getId() == R.id.checkBox) {
            if (checkBox.isChecked()) {
                button.setEnabled(false);
                editText.setEnabled(false);
                long sleepTimeInMillis = Long.parseLong(editText.getText().toString()) * 1000;
                if (sleepTimeInMillis < 10000) {
                    editText.setText("10");
                }
                new AutomaticLoopThread().start();
            } else {
                button.setEnabled(true);
                editText.setEnabled(true);
            }
        }
    }

    private class AutomaticLoopThread extends Thread {
        public void run() {
            while (true && checkBox.isChecked()) {
                try {
                    new GetServerTimeThread(bluetoothAdapter, device, true).start();
                    long sleepTimeInMillis = Long.parseLong(editText.getText().toString()) * 1000;
                    Thread.sleep(sleepTimeInMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class GetServerTimeThread extends Thread {
        private BluetoothAdapter bluetoothAdapter;
        private BluetoothSocket socket = null;
        private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private boolean automatic;
        private InputStream inputStream = null;

        public GetServerTimeThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device, boolean automatic) {
            this.bluetoothAdapter = bluetoothAdapter;
            this.automatic = automatic;
        }

        public void run() {
            getServerTime();
        }

        private void getServerTime() {
            try {
                try {
                    socket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
                    bluetoothAdapter.cancelDiscovery();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.connect();
                inputStream = socket.getInputStream();
                byte timeBytes[] = new byte[1024];
                inputStream.read(timeBytes);
                timeBytesString = new String(timeBytes);
                timeBytesString += (++counter);
                updateTime();
                cleanup();
            } catch (IOException connectException) {
                connectException.printStackTrace();
            }
        }

        private void cleanup() {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
        }
    }
}
