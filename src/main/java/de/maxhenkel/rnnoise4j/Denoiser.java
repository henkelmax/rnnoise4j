package de.maxhenkel.rnnoise4j;

import java.io.IOException;

public class Denoiser implements AutoCloseable {

    private long denoiser;

    public Denoiser() throws IOException, UnknownPlatformException {
        RNNoise.load();
        denoiser = createDenoiser();
    }

    private static native long createDenoiser();

    public native short[] denoise(short[] input);

    private native long destroyDenoiser();

    @Override
    public void close() {
        destroyDenoiser();
        denoiser = 0L;
    }

    public boolean isClosed() {
        return denoiser == 0L;
    }

}
