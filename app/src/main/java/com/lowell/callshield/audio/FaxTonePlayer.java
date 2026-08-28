package com.lowell.callshield.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

/** Plays the standard 1100 Hz fax calling tone (CNG) for thirty seconds. */
public final class FaxTonePlayer implements AutoCloseable {
    static final int SAMPLE_RATE = 8_000;
    static final int DURATION_SECONDS = 30;
    private static final double FREQUENCY_HZ = 1_100.0;
    private static final double TONE_SECONDS = 0.5;
    private static final double CYCLE_SECONDS = 3.5;
    private static final double AMPLITUDE = 0.22;

    private AudioTrack track;
    private Runnable completion;

    public void play(Runnable onComplete) {
        close();
        short[] samples = createSamples();
        completion = onComplete;
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.length * Short.BYTES)
                .build();
        int written = track.write(samples, 0, samples.length);
        if (written != samples.length) {
            close();
            throw new IllegalStateException("Could not load fax tone audio");
        }
        track.setNotificationMarkerPosition(samples.length - 1);
        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack ignored) {
                Runnable callback = completion;
                close();
                if (callback != null) callback.run();
            }

            @Override
            public void onPeriodicNotification(AudioTrack ignored) {}
        }, new Handler(Looper.getMainLooper()));
        track.play();
    }

    static short[] createSamples() {
        int sampleCount = SAMPLE_RATE * DURATION_SECONDS;
        short[] samples = new short[sampleCount];
        for (int index = 0; index < sampleCount; index++) {
            double elapsed = index / (double) SAMPLE_RATE;
            if (elapsed % CYCLE_SECONDS < TONE_SECONDS) {
                double wave = Math.sin(2.0 * Math.PI * FREQUENCY_HZ * elapsed);
                samples[index] = (short) (wave * Short.MAX_VALUE * AMPLITUDE);
            }
        }
        return samples;
    }

    @Override
    public void close() {
        completion = null;
        if (track != null) {
            try {
                track.stop();
            } catch (IllegalStateException ignored) {
                // A track that did not reach PLAYSTATE_PLAYING does not need stopping.
            }
            track.release();
            track = null;
        }
    }
}
