package com.app.airbridgeandroid;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class SendActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1;
    private Uri selectedFileUri;
    private String selectedFilePath;
    private long fileSize;

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

                if (selectedFileUri == null) {
                    Toast.makeText(SendActivity.this, "Please select a file first", Toast.LENGTH_SHORT).show();
                } else {
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
                selectedFilePath = getFileNameAndSize(selectedFileUri)[0];
                fileSize = Long.parseLong(getFileNameAndSize(selectedFileUri)[1]);

                Toast.makeText(this, "Selected File: " + selectedFilePath + " File size: " + fileSize, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Function to get the file name from Uri
    private String[] getFileNameAndSize(Uri uri) {
        String fileName = null;
        String fileSize = null;
        String[] result = new String[2];

        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    fileSize = cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }

        result[0] = fileName;
        result[1] = fileSize;
        return result;
    }

    // Function to send files to the server
    private void sendFileToServer() {

        try {
            OutputStream outputStream = SocketHandler.getSocket().getOutputStream();
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            String header = String.format("%-100s%-13d", selectedFilePath, fileSize);
            byte[] headerBytes = header.getBytes("UTF-8");

            outputStream.write(headerBytes);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            runOnUiThread(() -> Toast.makeText(SendActivity.this, "File sent successfully!", Toast.LENGTH_SHORT).show());
        } catch (FileNotFoundException e) {
            runOnUiThread(() -> Toast.makeText(SendActivity.this, "Error: File not found", Toast.LENGTH_SHORT).show());
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            runOnUiThread(() -> Toast.makeText(SendActivity.this, "Error: File encoding not supported", Toast.LENGTH_SHORT).show());
            throw new RuntimeException(e);
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(SendActivity.this, "Error: Something went wrong", Toast.LENGTH_SHORT).show());
        }
    }
}
