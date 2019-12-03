package com.example.a86007077.extmicrec2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;
import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    private Button play, stop, record, stop_play;
    private MediaRecorder myAudioRecorder;
    private AudioRecord recorder;
    private TextView raw_value;
    //private AudioRecord recorder;
    private String outputFile;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int minBuffSize, SAMPLING_RATE=8000;
    //private short[] audiodata;
    Thread mThread,wrThread;
    Timer timer;
    Boolean isRecording;
    int rec;
    private File file;
    private final int REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);
        record = (Button) findViewById(R.id.record);
        stop_play = (Button) findViewById(R.id.stop_play);
        raw_value = (TextView) findViewById(R.id.sensor_value);
        stop.setEnabled(false);
        play.setEnabled(false);
        stop_play.setEnabled(false);
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() ;
        //outerrFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "error.txt";
       // myAudioRecorder = new MediaRecorder();

       // myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      //  myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      //  myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
     //   mediaPlayer = new MediaPlayer();
        //myAudioRecorder.setOutputFile(outputFile);
        minBuffSize = android.media.AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        rec = 0;



        if(Build.VERSION.SDK_INT >=23){
            String[] permissions = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            if (ContextCompat.checkSelfPermission(this, permissions[0])
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                System.out.println("mic permission not granted");
                ActivityCompat.requestPermissions(this,
                        permissions,
                        REQUEST_CODE);

            }else {
                Toast.makeText(getApplicationContext(), "Already Granted", Toast.LENGTH_LONG).show();
            }
            /*
            if (ContextCompat.checkSelfPermission(this, permissions[1])
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                System.out.println("storage permission not granted");
            }*/

        }

        recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLING_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(minBuffSize)
                .build();

        record.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                record.setEnabled(false);
                stop.setEnabled(true);
                stop_play.setEnabled(false);
                play.setEnabled(false);

                timer = new Timer();
                recorder.startRecording();
                isRecording = true;

                wrThread =new Thread(new Runnable() {
                    @Override
                    public void run() {
                        rec +=1;
                        FileOutputStream os = null;
                        ByteArrayOutputStream recordingData = new ByteArrayOutputStream();
                        DataOutputStream dataStream = new DataOutputStream(recordingData);
                        //read data
                        byte[] audiobyte = new byte[minBuffSize/2];

                        try {
                            file = new File(outputFile, "recording"+String.valueOf(rec)+".pcm");
                            while(file.exists()){
                                rec++;
                                file = new File(outputFile, "recording"+String.valueOf(rec)+".pcm");
                            }
                            os = new FileOutputStream(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        while (isRecording) {
                            recorder.read(audiobyte,0,minBuffSize/2);

                            try {
                                os.write(audiobyte, 0, minBuffSize / 2);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try
                        {
                            dataStream.flush();
                            dataStream.close();
                            if (os != null)
                                os.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }

                    }
                });


                mThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {

                                short[] audiodata = new short[minBuffSize/4];

                                int bufferResults = recorder.read(audiodata,0,minBuffSize/4);

                                double sum = 0;
                                for(int i=0;i<bufferResults;i++){
                                    sum +=abs(audiodata[i]);

                                }

                                //double p2 = abs(audiodata[0]);
                                double p2 = sum/bufferResults;
                                double decibel;

                                if (p2==0)
                                    decibel = Double.NEGATIVE_INFINITY;
                                else
                                    decibel = 20.0*Math.log10(p2/32767)+149;

                                String dec = String.format("%.1f",decibel);
                                //System.out.println(dec);

                                TextView raw_value = findViewById(R.id.sensor_value);
                                raw_value.setText(String.valueOf(String.valueOf(dec)));

                            }
                        },0,300);

                    }
                });
                /*
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        mThread.start();
                    }
                },0,10000);
                */

                mThread.start();
                wrThread.start();

//                myAudioRecorder.start();
            }
        });


        stop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                try {
                    myAudioRecorder.stop();
                } catch (RuntimeException e){
                    mFile.delete();
                } finally{
                    myAudioRecorder.release();
                    myAudioRecorder=null;
                }
                */
                isRecording = false;
                timer.cancel();
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                recorder.stop();
              //  myAudioRecorder.release();
              //  myAudioRecorder = null;

                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(false);
                stop_play.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Audio Recorder stopped", Toast.LENGTH_LONG).show();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    mediaPlayer.setDataSource(outputFile+"recording"+String.valueOf(rec)+".pcm");
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Toast.makeText(getApplicationContext(), "Playing Audio", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    // make something
                }
                stop_play.setEnabled(true);
            }
        });
        stop_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.release();
                mediaPlayer = null;
                stop_play.setEnabled(false);
                play.setEnabled(false);
            }
        });

    }

}
