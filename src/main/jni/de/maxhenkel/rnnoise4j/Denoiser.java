package de.maxhenkel.rnnoise4j;

import de.maxhenkel.nativeutils.NativeInitializer;
import de.maxhenkel.nativeutils.UnknownPlatformException;

import java.io.IOException;
import java.io.InputStream;

public class Denoiser extends DenoiserBase {

    public Denoiser() throws IOException, UnknownPlatformException {
        synchronized (Denoiser.class) {
            if (loadError != null) {
                throw new IOException(loadError.getMessage());
            }
            NativeInitializer.load("librnnoise4j");
            if (weights == null) {
                try (InputStream in = Denoiser.class.getResourceAsStream(WEIGHTS_PATH)) {
                    if (in == null) {
                        throw new IOException("Could not find weights");
                    }
                    weights = readAllBytes(in);
                } catch (IOException e) {
                    loadError = e;
                    throw e;
                }
            }
            pointer = createDenoiser0(weights);
        }
    }

    private static native long createDenoiser0(byte[] model);

    private static native int getFrameSize0();

    @Override
    public int getFrameSize() {
        synchronized (this) {
            return getFrameSize0();
        }
    }

    private native short[] denoise0(long denoiserPointer, short[] input);

    @Override
    public short[] denoise(short[] input) {
        synchronized (this) {
            return denoise0(pointer, input);
        }
    }

    private native float denoiseInPlace0(long denoiserPointer, short[] input);

    @Override
    public float denoiseInPlace(short[] input) {
        synchronized (this) {
            return denoiseInPlace0(pointer, input);
        }
    }

    private native float getSpeechProbability0(long denoiserPointer, short[] input);

    @Override
    public float getSpeechProbability(short[] input) {
        synchronized (this) {
            return getSpeechProbability0(pointer, input);
        }
    }

    private native void destroyDenoiser0(long denoiserPointer);

    @Override
    public void close() {
        synchronized (this) {
            destroyDenoiser0(pointer);
            pointer = 0L;
        }
    }

    @Override
    public boolean isClosed() {
        synchronized (this) {
            return pointer == 0L;
        }
    }

}
