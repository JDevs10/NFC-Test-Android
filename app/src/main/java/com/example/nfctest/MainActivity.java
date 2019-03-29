package com.example.nfctest;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import static android.nfc.NdefRecord.createMime;
import static android.nfc.NdefRecord.createTextRecord;

/*
* Android Model : XXXXXXXXX
* */

public class MainActivity extends AppCompatActivity  {

    public static final String NFC = "android.permission.NFC";

    private NfcAdapter nfcAdapter;
    private EditText textView;
    private ToggleButton toggleButton_show;
    private boolean NFC_checker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for available NFC Adapter
        NFC_checker();

        textView = (EditText) findViewById(R.id.textView_nfcShow);
        toggleButton_show = (ToggleButton)findViewById(R.id.toggleButton_show);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        toggleButton_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
            }
        });

        // Register callback
        //nfcAdapter.setNdefPushMessageCallback(this, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        NFC_checker();
        if (NFC_checker){
            enableForegroundDispatchSystem();
        }else {
            Toast.makeText(this, "NFC not available !\n" +
                    "To activate NFC on your device :\n" +
                    "Go to => Settings > Wireless & Networks: More > NFC > turn off to on.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        super.onNewIntent(intent);

        if (NFC_checker) {
            if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
                Toast.makeText(this, "NFC Intent !", Toast.LENGTH_LONG).show();

                if (toggleButton_show.isChecked()){
                    Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                    if (parcelables != null && parcelables.length > 0){
                        readTextFromTag((NdefMessage)parcelables[0]);
                    }else{
                        Toast.makeText(this, "No NFC Message found !", Toast.LENGTH_LONG).show();
                    }

                }else {
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    NdefMessage ndefMessage = createNdefMessage(textView.getText()+"");
                    writeNdefMessage(tag, ndefMessage);
                }


            }
        }
    }

    private void NFC_checker(){
        // Check if NFC is available on the device
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && nfcAdapter.isEnabled()){
            NFC_checker = true;
            Toast.makeText(this, "NFC available !", Toast.LENGTH_LONG).show();
        }else {
            NFC_checker = false;
            Toast.makeText(this, "NFC not available !\n" +
                    "To activate NFC on your device :\n" +
                    "Go to => Settings > Wireless & Networks: More > NFC > turn off to on.", Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void enableForegroundDispatchSystem(){

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilters = new IntentFilter[] {};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void disableForegroundDispatchSystem(){

        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage msg){
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null){
                Toast.makeText(this, "Tag is not ndef Formatable", Toast.LENGTH_SHORT).show();
            }

            ndefFormatable.connect();
            ndefFormatable.format(msg);
            ndefFormatable.close();

        }catch (Exception e){
            Log.e("Format Tag : ", e.getMessage());
        }
    }

    private void writeNdefMessage(Tag tag, NdefMessage msg){
        try {
            if (tag == null){
                Toast.makeText(this, "Tag object cannot be null !", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);
            if (ndef == null){
                // format tag with the ndef format and writes the massage
                formatTag(tag, msg);
            }else{
                ndef.connect();
                if (!ndef.isWritable()){
                    Toast.makeText(this, "Tag object is not writable !", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(msg);
                ndef.close();
                Toast.makeText(this, "Tag written !", Toast.LENGTH_LONG).show();
            }

        }catch (Exception e){
            Log.e("Write Tag msg : ", e.getMessage());
        }
    }

    private NdefRecord createTextRecord(String content){
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes();

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payLoad = new ByteArrayOutputStream(1 + languageSize + textLength);

            payLoad.write((byte) (languageSize & 0x1F));
            payLoad.write(language, 0, languageSize);
            payLoad.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payLoad.toByteArray());

        }catch (UnsupportedEncodingException e){
            Log.e("Create msg Record : ", e.getMessage());
        }

        return null;
    }

    private NdefMessage createNdefMessage(String content){

        NdefRecord ndefRecord = createTextRecord(content);
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return ndefMessage;
    }

    /*Read NFC Tag data*/
    private void readTextFromTag(NdefMessage ndefMessage) {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if (ndefRecords != null & ndefRecords.length > 0){
            NdefRecord ndefRecord = ndefRecords[0];
            String tagContent = getTextFromNdefRecord(ndefRecord);
            textView.setText(tagContent);

        }else {
            Toast.makeText(this, "No NFC records found !", Toast.LENGTH_LONG).show();
        }
    }

    private String getTextFromNdefRecord(NdefRecord ndefRecord){

        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1, payload.length - languageSize - 1, textEncoding);
        }catch (UnsupportedEncodingException e){
            Log.e("getTextFromNdefRecord :", " "+e.getMessage(), e);
        }
        return tagContent;
    }

}
