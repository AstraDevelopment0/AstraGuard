package dev.astra.guard;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;

public class LibraryDownloader {

    private static final String URL_SQLITE = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar";

    public static void downloadIfNotExists(File libraryFolder) throws IOException {
        if (!libraryFolder.exists() && !libraryFolder.mkdirs()) {
            throw new IOException("Failed to create lib folder: " + libraryFolder.getAbsolutePath());
        }

        File jarFile = new File(libraryFolder, "sqlite-jdbc.jar");
        if (jarFile.exists()) return;

        try (InputStream in = new URL(URL_SQLITE).openStream()) {
            Files.copy(in, jarFile.toPath());
            Main.getInstance().getLogger().info("Downloaded sqlite-jdbc.jar");
        }
    }
}
