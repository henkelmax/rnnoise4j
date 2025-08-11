package de.maxhenkel.rnnoise4j;

public class TestUtils {

    public static short[] generateAudio(double[] frequencies, int sampleRate, double seconds) {
        int n = (int) Math.round(seconds * sampleRate);
        double attack = 0.02D; // 20 ms fade-in
        double release = 0.02D; // 20 ms fade-out
        int aSamp = (int) (attack * sampleRate);
        int rSamp = (int) (release * sampleRate);

        short[] out = new short[n];
        double perVoiceAmp = 0.6D / frequencies.length;
        double twoPiOverSr = 2D * Math.PI / sampleRate;

        for (int i = 0; i < n; i++) {
            double env = 1D;
            if (i < aSamp) {
                env = i / (double) aSamp;
            } else if (i > n - rSamp) {
                env = (n - i) / (double) rSamp;
            }

            double sample = 0D;
            for (double f : frequencies) {
                sample += perVoiceAmp * Math.sin(twoPiOverSr * f * i);
            }
            sample *= env;

            if (sample > 1D) {
                sample = 1D;
            }
            if (sample < -1D) {
                sample = -1D;
            }
            out[i] = (short) Math.round(sample * Short.MAX_VALUE);
        }
        return out;
    }

}
