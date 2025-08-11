package de.maxhenkel.rnnoise4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class DenoiserTest {

    @Test
    @DisplayName("Denoise")
    void denoise() throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            short[] shorts = TestUtils.generateAudio(new double[]{440D, 554.37D, 659.25D}, 48000, 1D);
            short[] buffer = new short[denoiser.getFrameSize()];
            for (int i = 0; i < shorts.length; i += buffer.length) {
                System.arraycopy(shorts, i, buffer, 0, buffer.length);
                short[] denoised = denoiser.denoise(buffer);
                assertEquals(denoiser.getFrameSize(), denoised.length);
            }
        }
    }

    @Test
    @DisplayName("Frame size")
    void frameSize() throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            assertEquals(480, denoiser.getFrameSize());
        }
    }

    @Test
    @DisplayName("Denoise empty array")
    void denoiseEmpty() throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            IllegalArgumentException e = assertThrowsExactly(IllegalArgumentException.class, () -> {
                denoiser.denoise(new short[0]);
            });
            assertEquals("Input array is empty", e.getMessage());
        }
    }

    @Test
    @DisplayName("Denoise null array")
    void denoiseNull() throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            IllegalArgumentException e = assertThrowsExactly(IllegalArgumentException.class, () -> {
                denoiser.denoise(null);
            });
            assertEquals("Input array is null", e.getMessage());
        }
    }

    @Test
    @DisplayName("Denoise invalid frame size")
    void denoiseInvalidFrameSize() throws IOException, UnknownPlatformException {
        try (Denoiser denoiser = new Denoiser()) {
            IllegalArgumentException e = assertThrowsExactly(IllegalArgumentException.class, () -> {
                denoiser.denoise(new short[denoiser.getFrameSize() - 1]);
            });
            assertEquals("Input array is not a multiple of the frame size", e.getMessage());
        }
    }

}
