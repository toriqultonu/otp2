package com.example.otp2;

import android.Manifest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView phoneTextView1, phoneTextView2;
    private Button registerButton;
    private SharedPreferences sharedPreferences;
    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request SMS permissions
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        }

        // Initialize views
        phoneTextView1 = findViewById(R.id.phoneTextView1);
        phoneTextView2 = findViewById(R.id.phoneTextView2);
        registerButton = findViewById(R.id.registerButton);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("PhoneNumbers", Context.MODE_PRIVATE);

        // Load saved phone numbers if available, otherwise TextViews remain empty
        loadSavedPhoneNumbers();

        // Set OnClickListener for the register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the phone number input dialog
                showPhoneNumberInputDialog();
            }
        });

        startService(new Intent(this, SmsListenerService.class));
    }

    // Load saved phone numbers from SharedPreferences
    private void loadSavedPhoneNumbers() {
        // Retrieve phone numbers from SharedPreferences
        String phoneNumber1 = sharedPreferences.getString("phone1", "");
        String phoneNumber2 = sharedPreferences.getString("phone2", "");

        // If the phone numbers exist in SharedPreferences, display them
        if (!phoneNumber1.isEmpty()) {
            phoneTextView1.setText(phoneNumber1);
        } else {
            phoneTextView1.setText(""); // Ensure it is empty if not set
        }

        if (!phoneNumber2.isEmpty()) {
            phoneTextView2.setText(phoneNumber2);
        } else {
            phoneTextView2.setText(""); // Ensure it is empty if not set
        }
    }

    // Show a dialog to input phone numbers, and load existing numbers if available
    private void showPhoneNumberInputDialog() {
        // Inflate the custom layout for the dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.phone_input_dialog, null);

        // Initialize the EditTexts from the dialog
        final EditText phoneInput1 = dialogView.findViewById(R.id.phoneInput1);
        final EditText phoneInput2 = dialogView.findViewById(R.id.phoneInput2);

        // Retrieve previously saved phone numbers
        String savedPhone1 = sharedPreferences.getString("phone1", "");
        String savedPhone2 = sharedPreferences.getString("phone2", "");

        // If phone numbers are saved, load them into the EditText fields for editing
        if (!savedPhone1.isEmpty()) {
            phoneInput1.setText(savedPhone1);
        }

        if (!savedPhone2.isEmpty()) {
            phoneInput2.setText(savedPhone2);
        }

        // Create an AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter Phone Numbers")
                .setView(dialogView)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the input phone numbers
                        String phoneNumber1 = phoneInput1.getText().toString();
                        String phoneNumber2 = phoneInput2.getText().toString();

                        // Validate input
                        if (!phoneNumber1.isEmpty() && !phoneNumber2.isEmpty()) {
                            // Save the phone numbers in SharedPreferences
                            savePhoneNumbers(phoneNumber1, phoneNumber2);

                            // Set the phone numbers to the TextViews
                            phoneTextView1.setText(phoneNumber1);
                            phoneTextView2.setText(phoneNumber2);

                            Toast.makeText(MainActivity.this, "Phone numbers saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter both phone numbers", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Show the dialog
        dialog.show();
    }

    // Save phone numbers in SharedPreferences
    private void savePhoneNumbers(String phone1, String phone2) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phone1", phone1);
        editor.putString("phone2", phone2);
        editor.apply();
    }

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE  // Add this for dual SIM support
    };

    private void checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(REQUIRED_PERMISSIONS, SMS_PERMISSION_CODE);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
