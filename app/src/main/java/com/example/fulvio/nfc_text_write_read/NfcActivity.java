package com.example.fulvio.nfc_text_write_read;


import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class NfcActivity extends Activity {

    NfcAdapter nfcAdapter;

    ToggleButton tglReadWrite;
    EditText txtTagContent;
    TextView tvReadDescription;
    TextView tvWriteDescription;
    ImageView ivNfcLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        tglReadWrite = (ToggleButton) findViewById(R.id.tglReadWrite);
        txtTagContent = (EditText) findViewById(R.id.txtTagContent);
        tvReadDescription = (TextView) findViewById(R.id.tvReadDescription);
        tvWriteDescription = (TextView) findViewById(R.id.tvWriteDescription);
        tvReadDescription.setVisibility(View.GONE);
        ivNfcLogo = (ImageView) findViewById(R.id.ivNfc);

        ivNfcLogo.setImageResource(R.drawable.nfclogo);

        tglReadWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtTagContent.setText("");
                if (tglReadWrite.isChecked()) {

                    tvWriteDescription.setVisibility(View.GONE);
                    tvReadDescription.setVisibility(View.VISIBLE);
                } else {
                    tvReadDescription.setVisibility(View.GONE);
                    tvWriteDescription.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /* BISOGNA DEFINIRE IL FOREGROUND DISPATCH SYSTEM TRA LA onResume() E LA onPause IN MODO TALE DI
    NON FAR RIAVVIARE L'APP AD OGNI LETTURA DI TAG */
    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter == null) {
            Toast.makeText(this, "Il dispositivo non supporta gli NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is not enabled please enabled NFC", Toast.LENGTH_LONG).show();
        }
        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
    }

    /*QUESTO METODO VA AD INTERCETTARE OGNI NUOVO INTENT LANCIATO E, A SECONDA SE SI E' IN MODALITÀ SCRITTURA/LETTURA,
     VA AD INVOCARE GLI APPOSITI METODI PASSANDOGLI DEGLI NDEFMESSAGE CREATI AD HOC */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this, R.string.strNfcIntent, Toast.LENGTH_SHORT).show();

            if (tglReadWrite.isChecked()) {
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                if (parcelables != null && parcelables.length > 0) {
                    readTextFromMessage((NdefMessage) parcelables[0]);
                } else {
                    Toast.makeText(this, R.string.strNoNdefMessagesFound, Toast.LENGTH_SHORT).show();
                }

            } else {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefMessage ndefMessage = createNdefMessage(txtTagContent.getText() + "");

                writeNdefMessage(tag, ndefMessage);
            }

        }
    }
/*QUESTO METODO RICEVE IL NDEFMESSAGE CREATO DAL METODO onNewIntent() E LO SPACCHETTA IN RECORD,
CHE INFINE CONVERTE IN STRINGHE E STAMPA A VIDEO */

    private void readTextFromMessage(NdefMessage ndefMessage) {

        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if (ndefRecords.length > 0) {

            NdefRecord ndefRecord = ndefRecords[0];

            String tagContent = getTextFromNdefRecord(ndefRecord);

            txtTagContent.setText(tagContent);

        } else {
            Toast.makeText(this, R.string.strNoNdefRecordFound, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_nfc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
/*ABILITO IL FOREGROUND DISPATCH PASSANDOGLI UN PENDING INTENT CHE IL S.O. PUÒ POPOLARE
CON I DETTAGLI DEL TAG QUANDO VIENE LETTO. IN PIU' DICHIARO UN INTENT FILTER PER FILTRARE GLI INTENT CHE VOGLIAMO INTERCETTARE */

    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, NfcActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    // DISABILITO IL FOREGROUND DISPATCH SYSTEM
    private void disableForegroundDispatchSystem() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    /*METODO INVOCATO QUANDO STIAMO PER SCRIVERE SU UN TAG, SERVE PER VERIFICARE SE IL TAG È NDEFFORMATABLE,
    SE NO, STAMPA CHE NON È FORMATTABILE, SE SI, SCRIVE IL TAG*/
    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, R.string.strTagIsNotNdefFormatable, Toast.LENGTH_SHORT).show();
                return;
            }


            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, R.string.strTagWritten, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

    }

    /* CONTROLLO SE IL TAG È AGGANCIATO AL DISPOSITIVO NFC E SE IL MESSAGGIO DA SCRIVERE NON È NULLO,
    ( SE E' NULLO OCCORRE RICORRERE A formatTag() )
     SUCCESSIVAMENTE INVOCO IL METODO PER SCRIVERE IL TAG, CON IL CONTROLLO PER SAPERE SE IL TAG  È SCRIVIBILE */
    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        try {

            if (tag == null) {
                Toast.makeText(this, R.string.strTagObjectCannotBeNull, Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null)
            {
                // FORMATTA IL TAG CON IL FORMATO NDEF E SCRIVI IL MESSAGGIO.
                formatTag(tag, ndefMessage);
            }
            else
            {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, R.string.strTagIsNotWritable, Toast.LENGTH_SHORT).show();

                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, R.string.strTagWritten, Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {
            Log.e("writeNdefMessage", e.getMessage());
        }

    }


    private NdefRecord createTextRecord(String content) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }

    /* RICEVE LA STRINGA DA SCRIVERE SUL TAG,  TRAMITE IL METODO PRECEDENTE (CREATE TEXT RECORD)
     CREA UN NDEFRECORD E NE ESTRAPOLA UN NDEFMESSAGE */
    private NdefMessage createNdefMessage(String content) {

        NdefRecord ndefRecord = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return ndefMessage;
    }


    /*QUESTO METODO, USATO PER LA LETTURA, RICEVE UN NDEFRECORD E SALVA IL CONTENUTO IN UNA STRINGA CHE RITORNA AL METODO CHIAMANTE*/
    public String getTextFromNdefRecord(NdefRecord ndefRecord) {
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding;
            if ((payload[0] & 128) == 0) textEncoding = "UTF-8";
            else textEncoding = "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }

        return tagContent;
    }

}
