package de.maxhenkel.rnnoise4j;

import de.maxhenkel.nativeutils.UnknownPlatformException;

import java.io.IOException;
import java.util.Arrays;

public class Benchmarks {

    public static void main(String[] args) throws IOException, UnknownPlatformException {
        String java = System.getProperty("java.version");
        System.out.println("Java version: " + java);
        short[] generated = TestUtils.generateAudio(new double[]{440D, 554.37D, 659.25D}, 48000, 2D);
        short[] testAudio = Arrays.copyOf(generated, 960 * 50);
        runTest("In place", () -> runInPlaceTest(testAudio));
        runTest("Denoise", () -> runDenoiseTest(testAudio));
        runTest("Speech probability", () -> runSpeechProbabilityTest(testAudio));
    }

    private static void runInPlaceTest(short[] testAudio) throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            if (testAudio.length % denoiser.getFrameSize() != 0) {
                throw new IllegalArgumentException("Test audio is not a multiple of the frame size");
            }
            short[] buffer = new short[testAudio.length];
            System.arraycopy(testAudio, 0, buffer, 0, buffer.length);
            denoiser.denoiseInPlace(buffer);
        }
    }

    private static void runDenoiseTest(short[] testAudio) throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            short[] buffer = new short[denoiser.getFrameSize()];
            for (int i = 0; i < testAudio.length; i += buffer.length) {
                System.arraycopy(testAudio, i, buffer, 0, buffer.length);
                short[] denoised = denoiser.denoise(buffer);
                assert denoiser.getFrameSize() == denoised.length;
            }
        }
    }

    private static void runSpeechProbabilityTest(short[] testAudio) throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            short[] buffer = new short[denoiser.getFrameSize()];
            for (int i = 0; i < testAudio.length; i += buffer.length) {
                System.arraycopy(testAudio, i, buffer, 0, buffer.length);
                denoiser.getSpeechProbability(buffer);
            }
        }
    }

    private static void runTest(String name, TestRunner runnable) throws IOException, UnknownPlatformException {
        long time = System.nanoTime();
        for (int i = 0; i < 1_000; i++) {
            runnable.run();
        }
        long nanoTime = System.nanoTime() - time;
        System.out.println("[" + name + "] Time: " + nanoTime / 1_000_000_000D + "s (" + nanoTime / 1_000_000D + "ms)");
    }

    private interface TestRunner {
        void run() throws IOException, UnknownPlatformException;
    }

}
