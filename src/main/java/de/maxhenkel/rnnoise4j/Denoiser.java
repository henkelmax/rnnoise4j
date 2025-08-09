package de.maxhenkel.rnnoise4j;

import java.io.IOException;

public class Denoiser implements AutoCloseable {

    private long pointer;

    public Denoiser() throws IOException, UnknownPlatformException {
        RNNoise.load();
        pointer = createDenoiser0();
    }

    private static native long createDenoiser0();

    private static native int getFrameSize0();

    /**
     * @return the frame size the denoiser is able to process
     */
    public int getFrameSize() {
        synchronized (this) {
            return getFrameSize0();
        }
    }

    private native short[] denoise0(long denoiserPointer, short[] input);

    /**
     * Denoises the given input
     *
     * @param input the input pcm samples - must be a multiple of the frame size
     * @return the denoised pcm samples
     */
    public short[] denoise(short[] input) {
        synchronized (this) {
            return denoise0(pointer, input);
        }
    }

    private native void destroyDenoiser0(long denoiserPointer);

    /**
     * Closes the denoiser - Not calling this will cause a memory leak!
     */
    @Override
    public void close() {
        synchronized (this) {
            destroyDenoiser0(pointer);
            pointer = 0L;
        }
    }

    /**
     * @return if the denoiser is closed
     */
    public boolean isClosed() {
        synchronized (this) {
            return pointer == 0L;
        }
    }

}
