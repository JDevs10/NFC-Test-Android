package com.example.nfctest;

import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

/*
* Android Model : XXXXXXXXX
* */

public class MainActivity extends AppCompatActivity {

    public static final String NFC = "android.permission.NFC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && nfcAdapter.isEnabled()){
            Toast.makeText(this, "NFC available", Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show();
        }
    }

}
