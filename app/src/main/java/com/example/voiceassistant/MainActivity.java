package com.example.voiceassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton button;
    ListView listView;
    SpeechRecognizer speechRecognizer;
    private chatArrayAdapter chatArrayAdapter;
    PyObject pyObj;
    String messageHolder;
    TextView spokenText;
    TextToSpeech tts;
    Dictionary<String, String> dictionary = new Hashtable<String, String>();
    Intent speechRecognizerIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET,
                        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CALL_PHONE}
                , PackageManager.PERMISSION_GRANTED);
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        pyObj = py.getModule("main");


        button = findViewById(R.id.mic_button);
        listView = findViewById(R.id.msg_list);
        spokenText = findViewById(R.id.spokenText);

        //button and text to speech code here
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        //Text to Speech

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("APP", "Lang not supported");
                    }
                }
            }
        });
        //Telephone Number
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET,
                    Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CALL_PHONE},PackageManager.PERMISSION_GRANTED);
        }else{
        String number = tMgr.getLine1Number();
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //button.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.mic_enabled_color));
                button.setVisibility(View.INVISIBLE);
                spokenText.setVisibility(View.VISIBLE);
                tts.stop();
                startSpeechToText(speechRecognizerIntent,true);
            }
        });

        chatArrayAdapter = new chatArrayAdapter(getApplicationContext(),R.layout.right_msg);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);
        //list code
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount()-1);
            }
        });

    }

    private class BackStuff extends AsyncTask<String,String,String>{


        @Override
        protected String doInBackground(String... strings) {
            Log.d("APP","String in background is "+ strings[0]);
            String query = strings[0];
            query = searchQuery(query,false);
            return query;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            messageHolder = s;
            tts.speak(s,TextToSpeech.QUEUE_ADD,null);
            sendChatMessage(false);
        }
    }

    class query{
        int code;
        String querys;

        public query(int code, String querys) {
            this.code = code;
            this.querys = querys;
        }
    }

    private class SeprateQuery extends AsyncTask<String,String,query>{
        @Override
        protected query doInBackground(String... strings) {
            String v_query = strings[0].toString();
            Log.e("APP","String recieved inn seprateQuery and is"+v_query);
            if(v_query.indexOf("wikipedia") != -1){
                Log.e("App","Query Contains wikipedia");
                v_query = v_query.replace("wikipedia ","");
                Log.e("App","Query Contains wikipedia and replaced with space new : "+v_query);
                return new query(1,v_query);
            }else if(v_query.indexOf("translate")!=-1){
                Log.e("App","Query Contains translate");
                return new query(2,v_query);
            }else if(v_query.indexOf("Call")!=-1){
                Log.e("APP","Calling Triggered");
                return new query()
            }
            return new query(-1,"null");
        }

        @Override
        protected void onPostExecute(query code) {
            super.onPostExecute(code);
            if( code.code == 1){
                Log.e("App","now seraching for code and fetching wikipedia");

                BackStuff back = new BackStuff();
                Log.e("App","Back Started and query is " + code.querys);
                back.execute(code.querys);
            }else if(code.code==2){
                String q = code.querys;
                Log.e("App","Seperated string is"+q);

                //Log.e("App","First Lang is" +firstLang +"Second is" + secondLang);
                messageHolder = "Enter the text to Translate";
                sendChatMessage(false);

                startSpeechToText(speechRecognizerIntent,false);
                PyObject py = pyObj.callAttr("translatorl",messageHolder,q);
                sendChatMessage(false);
                //speechRecognizer.startListening(speechRecognizerIntent,false);
            }
        }
    }

    private void sendChatMessage(boolean side){
        if(side != true){
            tts.speak(messageHolder,TextToSpeech.QUEUE_ADD,null);
        }
        chatArrayAdapter.add(new ChatMessage(side,messageHolder));
    }

    private void startSpeechToText(Intent speechRecognizerIntent,boolean forbtn){

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                tts.stop();
            }
            @Override
            public void onBeginningOfSpeech() {            }
            @Override
            public void onRmsChanged(float v) {            }
            @Override
            public void onBufferReceived(byte[] bytes) {           }
            @Override
            public void onEndOfSpeech() {
                //button.setColorFilter(ContextCompat.getColor(getApplicationContext(),R.color.mic_disabled_color));
                button.setVisibility(View.VISIBLE);
                spokenText.setVisibility(View.INVISIBLE);
                spokenText.setText("Listening...");
            }

            @Override
            public void onError(int i) {            }
            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> result = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if(result!= null){
                    String message = result.get(0);
                    messageHolder = message;
                    sendChatMessage(true);
                    if(forbtn == true) {
                        SeprateQuery queryCode = new SeprateQuery();
                        queryCode.execute(message);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                String[] resultss = bundle.getStringArray(speechRecognizer.RESULTS_RECOGNITION);
                ArrayList<String> results = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if(results!=null){
                    spokenText.setText(results.get(0));
                }

            }
            @Override
            public void onEvent(int i, Bundle bundle) {            }
        });

        speechRecognizer.startListening(speechRecognizerIntent);
        //searchQuery();

    }

    private String searchQuery(String strings, boolean b){
        try {
            Log.e("APP","Searching query");
            PyObject py = pyObj.callAttr("wikipediaSearch", strings);
            Log.e("APP","Getting the Result String"+py.toString());
            return py.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "Null";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.stop();
        tts.shutdown();
    }


}
//TODO: Add volume for speech recognizer and speed
//TODO: Add Calling for speech recognizer and speed
//TODO: Add Email for speech recognizer and speed
//TODO: Add notes Making for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed
//TODO: Add volume for speech recognizer and speed

