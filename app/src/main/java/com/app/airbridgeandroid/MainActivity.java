package com.app.airbridgeandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity  extends AppCompatActivity {
    private static String SERVER_IP = null;
    private static int SERVER_PORT = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendActivityButton = findViewById(R.id.sendActivity);
        Button receiveActivityButton = findViewById(R.id.receiveActivity);
        Button connectActivityButton = findViewById(R.id.connectServer);

        connectActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(() -> connectToServer()).start();
            }
        });

        sendActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkConnection()) {
                    Intent intent = new Intent(MainActivity.this, SendActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please connect to server first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        receiveActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkConnection()) {
                    Intent intent = new Intent(MainActivity.this, ReceiveActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please connect to server first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean checkConnection() {
        return SocketHandler.getSocket() != null && SocketHandler.getSocket().isConnected();
    }

    private void connectToServer() {
        if(SocketHandler.getSocket() != null && SocketHandler.getSocket().isConnected()) {
            try {
                SocketHandler.getSocket().close();
                boolean isClosed = SocketHandler.getSocket().isClosed();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Something went wrong while disconnecting the server", Toast.LENGTH_SHORT).show());
            }

            return;
        }

        EditText ipAddressBox = findViewById(R.id.ipAddressBox);
        EditText portNumberBox = findViewById(R.id.portNumberBox);

        SERVER_IP = ipAddressBox.getText().toString();
        String serverPortString = portNumberBox.getText().toString();

        if(SERVER_IP == null || SERVER_IP.equals("") || serverPortString == null || serverPortString.equals("")) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Server IP address and port number must not be empty", Toast.LENGTH_SHORT).show());
            return;
        }

        SERVER_PORT = Integer.parseInt(serverPortString);

        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            if(socket.isConnected()) {
                SocketHandler.setSocket(socket);
                Button connectActivityButton = findViewById(R.id.connectServer);
                connectActivityButton.setText("Disconnect");

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection successful", Toast.LENGTH_SHORT).show());
            }
        } catch(SocketException e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Not able to connect to server", Toast.LENGTH_SHORT).show());
        } catch(UnknownHostException e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: Please enter valid IP address and ", Toast.LENGTH_SHORT).show());
        } catch(Exception e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}