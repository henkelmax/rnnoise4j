package de.maxhenkel.rnnoise4j;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.io.File;

public interface RNNoise extends Library {

    RNNoise INSTANCE = Native.load(NativeLibrary.getInstance(LibraryLoader.getPath()).getFile().getAbsolutePath(), RNNoise.class);

    int rnnoise_get_size();

    int rnnoise_get_frame_size();

    int rnnoise_init(Pointer state, Pointer model);

    Pointer rnnoise_create(Pointer model);

    void rnnoise_destroy(Pointer state);

    float rnnoise_process_frame(Pointer state, float[] out, float[] in);

    Pointer rnnoise_model_from_file(File file);

    void rnnoise_model_free(Pointer model);

}
