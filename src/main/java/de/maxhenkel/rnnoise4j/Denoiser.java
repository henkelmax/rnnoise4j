package de.maxhenkel.rnnoise4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Denoiser implements AutoCloseable {

    public static final String WEIGHTS_PATH = "/rnnoise/weights_blob.bin";

    private static boolean loadError;
    private static byte[] weights;

    private long pointer;

    public Denoiser() throws IOException, UnknownPlatformException {
        synchronized (Denoiser.class) {
            if (loadError) {
                throw new IOException("Weights could not be loaded");
            }
            RNNoise.load();
            if (weights == null) {
                try (InputStream in = Denoiser.class.getResourceAsStream(WEIGHTS_PATH)) {
                    if (in == null) {
                        throw new IOException("Could not find weights");
                    }
                    weights = readAllBytes(in);
                } catch (IOException e) {
                    loadError = true;
                    throw e;
                }
            }
            pointer = createDenoiser0(weights);
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static native long createDenoiser0(byte[] model);

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
