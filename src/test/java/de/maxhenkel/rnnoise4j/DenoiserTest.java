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
        Denoiser denoiser = new Denoiser();
        short[] denoised = denoiser.denoise(new short[denoiser.getFrameSize() * 4]);
        denoiser.close();
        assertEquals(denoiser.getFrameSize() * 4, denoised.length);
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
