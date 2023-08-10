package de.maxhenkel.rnnoise4j;

import java.io.IOException;

public class Denoiser implements AutoCloseable {

    private long denoiser;

    public Denoiser() throws IOException, UnknownPlatformException {
        RNNoise.load();
        denoiser = createDenoiser0();
    }

    private static native long createDenoiser0();

    private native short[] denoise0(short[] input);

    public short[] denoise(short[] input) {
        synchronized (this) {
            return denoise0(input);
        }
    }

    private native long destroyDenoiser0();

    @Override
    public void close() {
        synchronized (this) {
            destroyDenoiser0();
            denoiser = 0L;
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return denoiser == 0L;
        }
    }

}
