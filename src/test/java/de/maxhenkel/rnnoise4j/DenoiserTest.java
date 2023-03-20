package de.maxhenkel.rnnoise4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DenoiserTest {

    @Test
    @DisplayName("Denoise")
    void encode() throws IOException, UnknownPlatformException {
        Denoiser denoiser = new Denoiser();
        short[] denoised = denoiser.denoise(new short[960]);
        denoiser.close();
        assertEquals(960, denoised.length);
    }

}
