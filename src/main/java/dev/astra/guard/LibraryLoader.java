package dev.astra.guard;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class LibraryLoader {

    private final JavaPlugin plugin;

    public LibraryLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllLibraries() {
        try {
            File libFolder = new File(plugin.getDataFolder(), "libraries");
            if (!libFolder.exists() && !libFolder.mkdirs()) return;

            File[] jarFiles = libFolder.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) return;

            URLClassLoader classLoader = (URLClassLoader) plugin.getClass().getClassLoader();
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);

            for (File jar : jarFiles) {
                addURL.invoke(classLoader, jar.toURI().toURL());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load libraries: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
