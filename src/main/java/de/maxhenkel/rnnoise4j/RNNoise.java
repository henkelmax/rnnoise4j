package de.maxhenkel.rnnoise4j;

import java.io.IOException;

class RNNoise {

    private static boolean loaded;
    private static Exception error;

    public static void load() throws UnknownPlatformException, IOException {
        if (loaded) {
            if (error != null) {
                if (error instanceof IOException) {
                    throw (IOException) error;
                } else if (error instanceof UnknownPlatformException) {
                    throw (UnknownPlatformException) error;
                }
                throw new RuntimeException(error);
            }
            return;
        }
        try {
            LibraryLoader.load("rnnoise4j");
            loaded = true;
        } catch (UnknownPlatformException | IOException e) {
            error = e;
            throw e;
        }
    }

    public static boolean isLoaded() {
        return loaded && error == null;
    }

}
