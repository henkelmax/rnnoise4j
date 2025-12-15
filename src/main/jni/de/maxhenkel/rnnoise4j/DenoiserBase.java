package de.maxhenkel.rnnoise4j;

import de.maxhenkel.nativeutils.UnknownPlatformException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class DenoiserBase implements AutoCloseable {

    protected static final String WEIGHTS_PATH = "/rnnoise/weights_blob.bin";

    protected static IOException loadError;
    protected static byte[] weights;

    protected long pointer;

    public DenoiserBase() throws IOException, UnknownPlatformException {

    }

    protected static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * @return the frame size the denoiser is able to process
     */
    public abstract int getFrameSize();

    /**
     * Denoises the given input.
     *
     * @param input the input pcm samples - must be a multiple of the frame size
     * @return the denoised pcm samples in a new array
     */
    public abstract short[] denoise(short[] input);

    /**
     * Denoises the given input in place.
     *
     * @param input the input pcm samples
     * @return the probability of speech (0-1)
     */
    public abstract float denoiseInPlace(short[] input);

    /**
     * Does the same as {@link #denoiseInPlace(short[])} but does not modify the input.
     * Used for getting the probability of speech without denoising.
     *
     * @param input the input pcm samples
     * @return the probability of speech (0-1)
     */
    public abstract float getSpeechProbability(short[] input);

    /**
     * Closes the denoiser - Not calling this will cause a memory leak!
     */
    @Override
    public abstract void close();

    /**
     * @return if the denoiser is closed
     */
    public abstract boolean isClosed();

}

