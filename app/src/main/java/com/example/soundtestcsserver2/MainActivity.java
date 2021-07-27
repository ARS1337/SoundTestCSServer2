package com.example.soundtestcsserver2;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final int SAMPLE_RATE = 48000;
    private final int CHANNEL_COUNT = AudioFormat.CHANNEL_OUT_STEREO;
    private final int ENCODING = AudioFormat.ENCODING_PCM_FLOAT; 
    int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_COUNT, ENCODING);
    AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
    AudioFormat audioFormat = new AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .build();
    AudioTrack audio = new AudioTrack(audioAttributes, audioFormat, bufferSize
            , AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

    PowerManager.WakeLock wl;
    volatile boolean bool1 = true;
    volatile boolean mainButtonBool = true;
    Thread aThread;
    ImageButton bu ;
    TextView txtView1;
    WifiManager.MulticastLock m;
    WifiManager w;
    final int one = 0xff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView1 = findViewById(R.id.textView);
        bu = findViewById(R.id.mainButton);
        w = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        m = w.createMulticastLock("kukka");
        m.acquire();
        getIPConnectionStatus();

        int sessionID = ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).generateAudioSessionId();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O  && (sessionID != AudioManager.ERROR)) {
                audio = new AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setSessionId(sessionID)
                        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                        .build();
        }


        getWakeLock();

        aThread = new Thread(new Runnable() {
            //@RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {

                audio.flush();
                byte[] buff1 = new byte[65535];
                DatagramPacket clPacket = new DatagramPacket(buff1, buff1.length);
                MulticastSocket ms = null;                                                  //enable now
                InetAddress group = null;
                try {
                    ms = new MulticastSocket(50001);                                       //enable now
                    group = InetAddress.getByName("224.0.0.3");                                //enable now
                    ms.joinGroup(group);                                                      //enable now
                    float[] out=null;                                                        //enable now
                    audio.play();
                    while (bool1) {
                        ms.receive(clPacket);
                        float[] newBF = convertInator(clPacket.getData(),clPacket.getLength());
                         audio.write(newBF, 0, newBF.length, AudioTrack.WRITE_BLOCKING);
                     //   Log.i("playing", "data size : "+ newBF.length + " data written : "+i+" diff "+ (newBF.length-i));
                    }
                    ms.leaveGroup(group);                                                         //enable now
                    ms.close();
                    if(m!=null&&m.isHeld()) m.release();                                           //
                } catch (Exception ex) {
                    Log.i("buff", Log.getStackTraceString(ex));
                    if (ms != null) {
                        ms.close();
                        if(m!=null&&m.isHeld()) m.release();                                       //
                    }
                }
                audio.flush();
                clPacket = null;
                if (ms != null) ms.close();
                ms = null;
                group = null;
                if(m!=null&&m.isHeld()) m.release();
            }
        }  );
        aThread.setPriority(Thread.MAX_PRIORITY);
        aThread.start();
    }


    public void getWakeLock() {
        wl = ((PowerManager) getSystemService(
                Context.POWER_SERVICE)).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "srdg:hgvjb");
        if(!wl.isHeld()) wl.acquire(9*60*60*1000L /*10 minutes*/);
    }

    public String getIP(int ipAddress){
        return String.format(Locale.ENGLISH,"%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
    }

    public void getIPConnectionStatus(){
        if(w.isWifiEnabled())txtView1.setText(String.format("%s%s", getApplicationContext().getString(R.string.enabled), getIP(w.getConnectionInfo().getIpAddress())));
        else txtView1.setText(getApplicationContext().getString(R.string.disabled));

    }

    public void onDestroy() {
        super.onDestroy();
        bool1 = false;
        mainButtonBool=false;
        //if(wl.isHeld()) wl.release();
        audio.release();
        audio = null;
        wl = null;
    }

    //Testing crashylytics; this method crashes o purpose
//    public void crasher(View v){
//        throw new RuntimeException("crashed lol");
//    }

    public float[] convertInator(byte[] b,int len) {
        float[] newB = new float[len / 4];
        for(int i = 0 ;i<len;i+=8){
            newB[i/4]=Float.intBitsToFloat((b[i+0]&one)|((b[i+1]&one)<<8)|((b[i+2]&one)<<16)|(b[i+3]<<24));
            newB[(i/4)+1]=Float.intBitsToFloat((b[i+4]&one)|((b[i+5]&one)<<8)|((b[i+6]&one)<<16)|(b[i+7]<<24));
        }
        return newB;
    }

    public void mainButtonClicked(View view){
        getIPConnectionStatus();
        mainButtonBool = !mainButtonBool;
        if(mainButtonBool){ bu.setImageResource(R.drawable.enable);audio.play();}
        else{ bu.setImageResource(R.drawable.disable);audio.pause();}
    }

    public void onPause() {
        if(wl.isHeld()) wl.release();
        super.onPause();
    }

    public void onResume() {
        if(!wl.isHeld()) wl.acquire(9*60*60*1000L /*10 minutes*/);
        super.onResume();
    }
}
