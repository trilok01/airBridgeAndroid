package com.app.airbridgeandroid;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class SendActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1;
    private Uri selectedFileUri;
    private String selectedFilePath;
    private static String RECEIVER_IP = null;
    private static int RECEIVER_PORT = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        Button btnSendFile = findViewById(R.id.btnSendFile);

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileSelector();
            }
        });

        btnSendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText ipAddressBox = findViewById(R.id.ipAddressBox);
                EditText portNumberBox = findViewById(R.id.portNumberBox);

                RECEIVER_IP = ipAddressBox.getText().toString();
                String receiverPortString = portNumberBox.getText().toString();

                if(RECEIVER_IP == null || RECEIVER_IP.equals("") || receiverPortString == null || receiverPortString.equals("")) {
                    Toast.makeText(SendActivity.this, "Receiver's IP address and port number must not be empty", Toast.LENGTH_SHORT).show();
                } else if (selectedFileUri == null) {
                    Toast.makeText(SendActivity.this, "Please select a file first", Toast.LENGTH_SHORT).show();
                } else {
                    RECEIVER_PORT = Integer.parseInt(receiverPortString);

                    new Thread(() -> sendFileToServer()).start();
                }
            }
        });
    }

    // Function to select a file from the Android device
    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                selectedFilePath = getFileName(selectedFileUri);
                Toast.makeText(this, "Selected File: " + selectedFilePath, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Function to get the file name from Uri
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // Function to send files to the server
    private void sendFileToServer() {
        String serverIp = RECEIVER_IP;
        int serverPort = RECEIVER_PORT;

        try {
            Socket socket = new Socket(serverIp, serverPort);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            long fileSize = inputStream.available();
            String header = String.format("%-100s%-8d", selectedFilePath, fileSize);
            byte[] headerBytes = header.getBytes("UTF-8");

            outputStream.write(headerBytes);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            bufferedInputStream.close();
            inputStream.close();

            runOnUiThread(() -> Toast.makeText(SendActivity.this, "File sent successfully!", Toast.LENGTH_SHORT).show());
        } catch(SocketException e) {
            runOnUiThread(() -> Toast.makeText(SendActivity.this, "Error: Not able to connect to server", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
//            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(SendActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
