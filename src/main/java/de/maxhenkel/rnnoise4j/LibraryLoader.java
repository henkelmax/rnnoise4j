package de.maxhenkel.rnnoise4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
        File tempDir = new File(getTempDir(), libraryName);
        tempDir.mkdirs();

        File tempFile = new File(tempDir, getLibraryName(libraryName));

        try (InputStream in = LibraryLoader.class.getClassLoader().getResourceAsStream(getResourcePath(libraryName))) {
            if (in == null) {
                throw new UnknownPlatformException(String.format("Could not find %s natives for platform %s", libraryName, getNativeFolderName()));
            }
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            System.load(tempFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            throw new UnknownPlatformException(String.format("Could not load %s natives for %s", libraryName, getNativeFolderName()), e);
        }
    }

}
