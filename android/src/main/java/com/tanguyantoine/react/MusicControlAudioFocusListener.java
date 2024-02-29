package com.tanguyantoine.react;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;

public class MusicControlAudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
    private final MusicControlEventEmitter emitter;
    private final MusicControlVolumeListener volume;

    private AudioManager mAudioManager;
    private AudioFocusRequest mFocusRequest;

    private boolean mPlayOnAudioFocus = false;

    MusicControlAudioFocusListener(ReactApplicationContext context, MusicControlEventEmitter emitter,
            MusicControlVolumeListener volume) {
        this.emitter = emitter;
        this.volume = volume;

        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (this.mAudioManager == null) {
            // Log an error or notify the user
            Log.e("MusicControl", "System service AUDIO_SERVICE is not available");
            // Alternatively, handle this error in a way that is appropriate for your app
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        if (mAudioManager == null) {
            Log.e("RNMC", "AudioManager is not initialized in onAudioFocusChange");
            if (emitter != null) {
                try {
                    emitter.onStop();
                } catch (Exception e) {
                    Log.e("RNMC", "Error calling onStop in emitter", e);
                }
            }
            return;
        }

        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            abandonAudioFocus();
            mPlayOnAudioFocus = false;
            emitter.onStop();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (MusicControlModule.INSTANCE.isPlaying()) {
                mPlayOnAudioFocus = true;
                emitter.onPause();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            volume.setCurrentVolume(40);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (volume.getCurrentVolume() != 100) {
                volume.setCurrentVolume(100);
            }
            if (mPlayOnAudioFocus) {
                emitter.onPlay();
            }
            mPlayOnAudioFocus = false;
        }
    }

    public void requestAudioFocus() {
        if (mAudioManager == null) {
            // Log an error or notify the user
            Log.e("MusicControl", "AudioManager is not initialized");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this).build();

            mAudioManager.requestAudioFocus(mFocusRequest);
        } else {
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public void abandonAudioFocus() {
        if (mAudioManager == null) {
            // Log an error or notify the user
            Log.e("MusicControl", "AudioManager is not initialized");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mAudioManager != null && mFocusRequest != null) {
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        } else if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(this);
        }
    }
}