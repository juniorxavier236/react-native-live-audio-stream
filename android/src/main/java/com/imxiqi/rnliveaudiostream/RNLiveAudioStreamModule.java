package com.imxiqi.rnliveaudiostream;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.Math;

// play sound
import com.facebook.react.bridge.ReadableArray;

import com.facebook.react.bridge.Callback;

import android.media.AudioTrack;
import android.media.AudioManager;
import android.os.Build;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

public class RNLiveAudioStreamModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

    private int sampleRateInHz;
    private int channelConfig;
    private int audioFormat;
    private int audioSource;

    private AudioRecord recorder;
    private int bufferSize;
    private boolean isRecording;

    private AudioManager audioManager;

    // play sound

    private static AudioTrack audioPlay;
    private boolean isFloat = false;
    private int bufferSizeTrack = 2048;

    public RNLiveAudioStreamModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        audioManager = ((AudioManager) this.reactContext.getSystemService(Context.AUDIO_SERVICE));

        //play sound
        if (audioPlay != null) {
            audioPlay.stop();
            audioPlay.release();
            audioPlay = null;
        }
    }

    // public static void wait(int ms) {
    //     try {
    //         Thread.sleep(ms);
    //     } catch (InterruptedException ex) {
    //         Thread.currentThread().interrupt();
    //     }
    // }

    @Override
    public String getName() {
        return "RNLiveAudioStream";
    }

    @ReactMethod
    public void initRecord(ReadableMap options) {
        sampleRateInHz = 8000;
        if (options.hasKey("sampleRate")) {
            sampleRateInHz = options.getInt("sampleRate");
        }

        channelConfig = AudioFormat.CHANNEL_IN_MONO;
        if (options.hasKey("channels")) {
            if (options.getInt("channels") == 2) {
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            }
        }

        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (options.hasKey("bitsPerSample")) {
            if (options.getInt("bitsPerSample") == 8) {
                audioFormat = AudioFormat.ENCODING_PCM_8BIT;
            }
        }

        audioSource = AudioSource.VOICE_RECOGNITION;
        if (options.hasKey("audioSource")) {
            audioSource = options.getInt("audioSource");
        }

        isRecording = false;
        eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

        if (options.hasKey("bufferSize")) {
            bufferSize = Math.max(bufferSize, options.getInt("bufferSize"));
        }

        int recordingBufferSize = bufferSize * 3;
        recorder = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, recordingBufferSize);
    }

    @ReactMethod
    public void startRecord() {
        isRecording = true;
        recorder.startRecording();

        Thread recordingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    int bytesRead;
                    int count = 0;
                    String base64Data;
                    byte[] buffer = new byte[bufferSize];
                    // audioTrack.play();
                     while (isRecording) {
                        bytesRead = recorder.read(buffer, 0, buffer.length);
                        // audioTrack.write(buffer, 0, buffer.length);
                        // skip first 2 buffers to eliminate "click sound"
                        if (bytesRead > 0 && ++count > 2) {
                            base64Data = Base64.encodeToString(buffer, Base64.NO_WRAP);
                            eventEmitter.emit("data", base64Data);
                        }

                     }
                    recorder.stop();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        recordingThread.start();
    }

    @ReactMethod
    public void stopRecord(Promise promise) {
        isRecording = false;
    }

    // play sound

    @ReactMethod
    public void initPlay(final ReadableMap options) {
        Thread playThread = new Thread(new Runnable() {
            public void run() {
                // int streamType = AudioManager.STREAM_MUSIC;
                int streamType = AudioManager.STREAM_VOICE_CALL;

                int sampleRateInHz = 8000;
                int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                int mode = AudioTrack.MODE_STREAM;
                if (options.hasKey("streamType")) {
                    streamType = options.getInt("streamType");
                }
                ;
                // if (options.hasKey("bitsPerChannel")) {
                // int bitsPerChannel = options.getInt("bitsPerChannel");
                // isFloat = false;

                // if (bitsPerChannel == 8) {
                // audioFormat = AudioFormat.ENCODING_PCM_8BIT;
                // } else if (bitsPerChannel == 32) {
                // audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
                // isFloat = true;
                // }
                // }
                if (options.hasKey("channelsPerFrame")) {
                    int channelsPerFrame = options.getInt("channelsPerFrame");

                    // every other case --> CHANNEL_IN_MONO
                    if (channelsPerFrame == 2) {
                        channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                    }
                }
                if (options.hasKey("sampleRate")) {
                    sampleRateInHz = options.getInt("sampleRate");
                }
                // if (options.hasKey("bufferSize")) {
                // bufferSize = options.getInt("bufferSize");
                // }
                // if (isFloat) {
                // bufferSizeTrack = AudioRecord.getMinBufferSize(8000,
                // AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                // Log.d("recorder", "setting buffer size " + bufferSize);
                // }
                audioPlay = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeTrack,
                        mode);
                audioPlay.play();
            }
        });

        playThread.start();
    }

    @ReactMethod
    public void playPlay() {
        if (audioPlay != null) {
            audioPlay.play();
        }
    }

    @ReactMethod
    public void stopPlay() {
        if (audioPlay != null) {
            audioPlay.stop();
            audioPlay.release();
            audioPlay = null;
        }
    }

    @ReactMethod
    public void pausePlay() {
        if (audioPlay != null) {
            audioPlay.pause();
        }
    }

    @ReactMethod
    public void setVolumePlay(float gain) {
        if (audioPlay != null) {
            audioPlay.setVolume(gain);
        }
    }

    // @ReactMethod
    // public void setSpeakerphoneon(final boolean enable) {
    //     if (enable != audioManager.isSpeakerphoneOn())  {
    //         audioManager.setSpeakerphoneOn(enable);
    //     }
    // }

    @ReactMethod
    public void setSpeakerphoneon(final boolean enable) {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if (enable != audioManager.isSpeakerphoneOn())  {
            Log.d("setSpeakerphoneOn", "setSpeakerphoneOn(): " + enable);
            audioManager.setSpeakerphoneOn(enable);
        }

    }


    @ReactMethod
    public void setMicrophoneMute(final boolean enable) {
        if (enable != audioManager.isMicrophoneMute())  {
            audioManager.setMicrophoneMute(enable);
        }
    }
    // @ReactMethod
    // public void write(String base64String) {
    // byte[] bytesArray = Base64.decode(base64String, Base64.NO_WRAP);
    // if (audioTrack != null && bytesArray != null) {
    // if (isFloat && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

    // FloatBuffer fb = ByteBuffer.wrap(bytesArray).asFloatBuffer();
    // float[] buffer = new float[fb.capacity()];
    // ByteBuffer.wrap(bytesArray).order(ByteOrder.nativeOrder()).asFloatBuffer().get(buffer);
    // try {
    // audioTrack.write(buffer, 0, buffer.length, AudioTrack.WRITE_BLOCKING);
    // } catch (Exception ignored) {
    // }
    // } else {
    // short[] buffer = byte2short(bytesArray);
    // try {
    // audioTrack.write(buffer, 0, bufferSize);
    // } catch (Exception ignored) {
    // }
    // }
    // }
    // }

    @ReactMethod
    public void writePlay(String base64String) {
        byte[] bytesArray = Base64.decode(base64String, Base64.NO_WRAP);
        // if (audioTrack != null) {
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

        // FloatBuffer fb = ByteBuffer.wrap(bytesArray).asFloatBuffer();
        // float[] buffer = new float[fb.capacity()];
        // ByteBuffer.wrap(bytesArray).order(ByteOrder.nativeOrder()).asFloatBuffer().get(buffer);
        // try {
        // audioTrack.write(buffer, 0, buffer.length, AudioTrack.WRITE_BLOCKING);
        // } catch (Exception ignored) {
        // }
        // } else {
        // short[] buffer = byte2short(bytesArray);
        // try {
        // audioTrack.write(buffer, 0, bufferSize);
        // } catch (Exception ignored) {
        // }
        // }
        try {
            short[] buffer = byte2short(bytesArray);
            // audioTrack.write(buffer, 0, bufferSizeTrack);
            audioPlay.write(buffer, 0, buffer.length);

        } catch (Exception ignored) {
            Log.d("write", "erro");
        }
        // }
    }

    public static short[] byte2short(byte[] paramArrayOfbyte) {
        short[] arrayOfShort = new short[paramArrayOfbyte.length / 2];
        for (int i = 0;; i += 2) {
            if (i >= paramArrayOfbyte.length)
                return arrayOfShort;
            byte b1 = paramArrayOfbyte[i];
            byte b2 = paramArrayOfbyte[i + 1];
            short s = (short) ((short) ((short) b1 & 0xFF) + (short) (b2 << 8));
            arrayOfShort[i / 2] = (short) s;
        }
    }

    // @Override
    // public void onRequestPermissionsResult(int requestCode, String permissions[],
    // int[] grantResults) {
    // switch (requestCode) {
    // case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: {
    // if (grantResults.length > 0
    // && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    // // permission granted -code

    // } else {

    // // permission not granted - code
    // }
    // }
    // }
    // }

}
