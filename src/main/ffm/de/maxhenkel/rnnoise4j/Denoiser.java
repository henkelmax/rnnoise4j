package de.maxhenkel.rnnoise4j;

import de.maxhenkel.nativeutils.NativeInitializer;
import de.maxhenkel.nativeutils.UnknownPlatformException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class Denoiser implements AutoCloseable {

    public static final String WEIGHTS_PATH = "/rnnoise/weights_blob.bin";

    private static final Linker LINKER = Linker.nativeLinker();

    private static MemorySegment MODEL;
    private static final FunctionDescriptor CREATE_DENOISER_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_BYTE),
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT)
    );
    private static MethodHandle CREATE_DENOISER_METHOD;
    private static final FunctionDescriptor GET_FRAME_SIZE_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.JAVA_INT
    );
    private static MethodHandle GET_FRAME_SIZE_METHOD;
    private static final FunctionDescriptor DENOISE_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_SHORT),
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_SHORT),
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT)
    );
    private static MethodHandle DENOISE_METHOD;
    private static final FunctionDescriptor DENOISE_IN_PLACE_DESCRIPTOR = FunctionDescriptor.of(
            ValueLayout.JAVA_FLOAT,
            ValueLayout.JAVA_BOOLEAN,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_SHORT),
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_INT)
    );
    private static MethodHandle DENOISE_IN_PLACE_METHOD;
    private static final FunctionDescriptor DESTROY_DESCRIPTOR = FunctionDescriptor.ofVoid(
            ValueLayout.JAVA_LONG
    );
    private static MethodHandle DESTROY_METHOD;
    private static final FunctionDescriptor FREE_DESCRIPTOR = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS
    );
    private static MethodHandle FREE_METHOD;

    private static IOException loadError;

    private long pointer;

    public Denoiser() throws IOException, UnknownPlatformException {
        synchronized (Denoiser.class) {
            if (loadError != null) {
                throw new IOException(loadError.getMessage());
            }
            NativeInitializer.load("librnnoise4j");
            if (MODEL == null) {
                try {
                    initFFM();
                } catch (IOException e) {
                    loadError = e;
                    throw e;
                }
            }
            pointer = createDenoiser();
        }
    }

    private static void initFFM() throws IOException {
        SymbolLookup lookup = SymbolLookup.loaderLookup();

        byte[] weights;
        try (InputStream in = Denoiser.class.getResourceAsStream(WEIGHTS_PATH)) {
            if (in == null) {
                throw new IOException("Could not find weights");
            }
            weights = readAllBytes(in);
        }

        MemorySegment createDenoiser = getSymbol(lookup, "rnnoise4j_create_denoiser");
        CREATE_DENOISER_METHOD = LINKER.downcallHandle(createDenoiser, CREATE_DENOISER_DESCRIPTOR);

        MemorySegment getFrameSize = getSymbol(lookup, "rnnoise4j_get_frame_size");
        GET_FRAME_SIZE_METHOD = LINKER.downcallHandle(getFrameSize, GET_FRAME_SIZE_DESCRIPTOR);

        MemorySegment denoise = getSymbol(lookup, "rnnoise4j_denoise");
        DENOISE_METHOD = LINKER.downcallHandle(denoise, DENOISE_DESCRIPTOR);

        MemorySegment denoiseInPlace = getSymbol(lookup, "rnnoise4j_denoise_in_place");
        DENOISE_IN_PLACE_METHOD = LINKER.downcallHandle(denoiseInPlace, DENOISE_IN_PLACE_DESCRIPTOR);

        MemorySegment destroy = getSymbol(lookup, "rnnoise4j_destroy_denoiser");
        DESTROY_METHOD = LINKER.downcallHandle(destroy, DESTROY_DESCRIPTOR);

        MemorySegment free = getSymbol(lookup, "rnnoise4j_free");
        FREE_METHOD = LINKER.downcallHandle(free, FREE_DESCRIPTOR);

        MODEL = Arena.global().allocateFrom(ValueLayout.JAVA_BYTE, weights);
    }

    private static MemorySegment getSymbol(SymbolLookup lookup, String name) {
        return lookup.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError(String.format("Symbol '%s' not found", name)));
    }

    private static long createDenoiser() throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment error = createError(arena);
            long pointer;
            try {
                pointer = (long) CREATE_DENOISER_METHOD.invokeExact(MODEL, (int) MODEL.byteSize(), error);
            } catch (Throwable e) {
                throw new IOException(e);
            }
            if (pointer == 0L) {
                throw getIOException(getError(error));
            }
            return pointer;
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

    /**
     * @return the frame size the denoiser is able to process
     */
    public int getFrameSize() {
        synchronized (this) {
            try {
                return (int) GET_FRAME_SIZE_METHOD.invokeExact();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Denoises the given input.
     *
     * @param input the input pcm samples - must be a multiple of the frame size
     * @return the denoised pcm samples in a new array
     */
    public short[] denoise(short[] input) {
        synchronized (this) {
            if (input == null) {
                throw new IllegalArgumentException("Input array is null");
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment error = createError(arena);
                MemorySegment denoised;
                try {
                    denoised = (MemorySegment) DENOISE_METHOD.invokeExact(pointer, arena.allocateFrom(ValueLayout.JAVA_SHORT, input), input.length, error);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                if (denoised.address() == 0L) {
                    throw getRuntimeException(getError(error));
                }
                short[] result = denoised.reinterpret((long) input.length * ValueLayout.JAVA_SHORT.byteSize()).toArray(ValueLayout.JAVA_SHORT);
                try {
                    FREE_METHOD.invokeExact(denoised);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return result;
            }
        }
    }

    /**
     * Denoises the given input in place.
     *
     * @param input the input pcm samples
     * @return the probability of speech (0-1)
     */
    public float denoiseInPlace(short[] input) {
        synchronized (this) {
            if (input == null) {
                throw new IllegalArgumentException("Input array is null");
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment error = createError(arena);
                MemorySegment inputSegment = arena.allocateFrom(ValueLayout.JAVA_SHORT, input);
                float result;
                try {
                    result = (float) DENOISE_IN_PLACE_METHOD.invokeExact(true, pointer, inputSegment, input.length, error);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                int err = getError(error);
                if (err != RNNOISE_NO_ERROR) {
                    throw getRuntimeException(err);
                }
                MemorySegment.copy(inputSegment, ValueLayout.JAVA_SHORT, 0L, input, 0, input.length);
                return result;
            }
        }
    }

    /**
     * Does the same as {@link #denoiseInPlace(short[])} but does not modify the input.
     * Used for getting the probability of speech without denoising.
     *
     * @param input the input pcm samples
     * @return the probability of speech (0-1)
     */
    public float getSpeechProbability(short[] input) {
        synchronized (this) {
            if (input == null) {
                throw new IllegalArgumentException("Input array is null");
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment error = createError(arena);
                float result;
                try {
                    result = (float) DENOISE_IN_PLACE_METHOD.invokeExact(false, pointer, arena.allocateFrom(ValueLayout.JAVA_SHORT, input), input.length, error);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                int err = getError(error);
                if (err != RNNOISE_NO_ERROR) {
                    throw getRuntimeException(err);
                }
                return result;
            }
        }
    }

    /**
     * Closes the denoiser - Not calling this will cause a memory leak!
     */
    @Override
    public void close() {
        synchronized (this) {
            try {
                DESTROY_METHOD.invokeExact(pointer);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                pointer = 0L;
            }
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

    private static final int RNNOISE_NO_ERROR = 0;
    private static final int RNNOISE_ERROR_COULD_NOT_LOAD_MODEL = 1001;
    private static final int RNNOISE_ERROR_COULD_NOT_CREATE_DENOISER = 1002;
    private static final int RNNOISE_ERROR_DENOISER_CLOSED = 1003;
    private static final int RNNOISE_ERROR_INPUT_ARRAY_NULL = 1004;
    private static final int RNNOISE_ERROR_INPUT_ARRAY_EMPTY = 1005;
    private static final int RNNOISE_ERROR_INPUT_ARRAY_NOT_MULTIPLE_OF_FRAME_SIZE = 1006;

    private static IOException getIOException(int errno) {
        return switch (errno) {
            case RNNOISE_ERROR_COULD_NOT_LOAD_MODEL -> new IOException("Could not load model");
            case RNNOISE_ERROR_COULD_NOT_CREATE_DENOISER -> new IOException("Could not create denoiser");
            default -> throw getRuntimeException(errno);
        };
    }

    private static RuntimeException getRuntimeException(int errno) {
        return switch (errno) {
            case RNNOISE_ERROR_DENOISER_CLOSED -> new RuntimeException("Denoiser is closed");
            case RNNOISE_ERROR_INPUT_ARRAY_NULL -> new IllegalArgumentException("Input array is null");
            case RNNOISE_ERROR_INPUT_ARRAY_EMPTY -> new IllegalArgumentException("Input array is empty");
            case RNNOISE_ERROR_INPUT_ARRAY_NOT_MULTIPLE_OF_FRAME_SIZE ->
                    new IllegalArgumentException("Input array is not a multiple of the frame size");
            default -> new IllegalArgumentException(String.format("Unknown error code: %s", errno));
        };
    }

    private static MemorySegment createError(Arena arena) {
        MemorySegment error = arena.allocate(ValueLayout.JAVA_INT);
        error.set(ValueLayout.JAVA_INT, 0L, 0);
        return error;
    }

    private static int getError(MemorySegment error) {
        return error.get(ValueLayout.JAVA_INT, 0L);
    }

}
