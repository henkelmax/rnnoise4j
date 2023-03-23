package de.maxhenkel.rnnoise4j;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class LibraryLoader {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    private static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    private static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    private static boolean isLinux() {
        return OS_NAME.contains("nux");
    }

    private static String getPlatform() throws UnknownPlatformException {
        if (isWindows()) {
            return "windows";
        } else if (isMac()) {
            return "mac";
        } else if (isLinux()) {
            return "linux";
        } else {
            throw new UnknownPlatformException(String.format("Unknown operating system: %s", OS_NAME));
        }
    }

    private static String getArchitecture() {
        switch (OS_ARCH) {
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "x86":
            case "x86_32":
                return "x86";
            case "amd64":
            case "x86_64":
            case "x86-64":
                return "x64";
            case "aarch64":
                return "aarch64";
            default:
                return OS_ARCH;
        }
    }

    private static String getLibraryExtension() throws UnknownPlatformException {
        if (isWindows()) {
            return "dll";
        } else if (isMac()) {
            return "dylib";
        } else if (isLinux()) {
            return "so";
        } else {
            throw new UnknownPlatformException(String.format("Unknown operating system: %s", OS_NAME));
        }
    }

    private static String getLibraryName(String name) throws UnknownPlatformException {
        if (isWindows()) {
            return String.format("%s.%s", name, getLibraryExtension());
        } else {
            return String.format("lib%s.%s", name, getLibraryExtension());
        }
    }

    private static String getNativeFolderName() throws UnknownPlatformException {
        return String.format("%s-%s", getPlatform(), getArchitecture());
    }

    private static String getResourcePath(String libName) throws UnknownPlatformException {
        return String.format("natives/%s/%s", getNativeFolderName(), getLibraryName(libName));
    }

    private static File getTempDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static void load(String libraryName) throws UnknownPlatformException, IOException {
        String resourcePath = getResourcePath(libraryName);

        @Nullable String md5 = null;
        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                throw new UnknownPlatformException(String.format("Could not find %s natives for platform %s", libraryName, getNativeFolderName()));
            }
            md5 = checksum(in);
        } catch (Exception ignored) {
        }

        File tempDir = new File(getTempDir(), md5 == null ? libraryName : String.format("%s-%s", libraryName, md5));
        tempDir.mkdirs();

        File tempFile = new File(tempDir, getLibraryName(libraryName));

        if (!tempFile.exists()) {
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    throw new UnknownPlatformException(String.format("Could not find %s natives for platform %s", libraryName, getNativeFolderName()));
                }
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        try {
            System.load(tempFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            throw new UnknownPlatformException(String.format("Could not load %s natives for %s", libraryName, getNativeFolderName()), e);
        }
    }

    @Nullable
    private static InputStream getResource(String path) {
        return LibraryLoader.class.getClassLoader().getResourceAsStream(path);
    }

    private static String checksum(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[1024];
        MessageDigest digest = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = inputStream.read(buffer);
            if (numRead > 0) {
                digest.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        inputStream.close();
        byte[] bytes = digest.digest();
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

}
