package com.hrzn.spark;

import com.hrzn.spark.natives.NativeExtractor;
import com.hrzn.spark.plugin.ISparkPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Given a config, this carries out the bootstrap process: extracting natives, loading plugins, injecting natives,
 * initializing the classloader and invoking the launch jar.
 */
public class SparkBootstrap {
    private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

    private final BootstrapConfig config;
    private final Collection<ISparkPlugin> plugins = new ArrayList<>();

    public SparkBootstrap(BootstrapConfig config) {
        this.config = config;
    }

    /**
     * Carries out the launch process given the provided config. This will block until the launch target exits.
     *
     * @throws Throwable if the launch process failed
     */
    public void launch() throws Throwable {
        Thread.currentThread().setContextClassLoader(SparkLauncher.CLASS_LOADER);

        this.clearNatives();
        try (NativeExtractor nativeExtractor = NativeExtractor.open(this.config.getLaunchJar())) {
            nativeExtractor.extractTo(this.config.getNativeDir());
        } catch (IOException e) {
            SparkLauncher.LOGGER.error("Failed to extract natives", e);
        }

        SparkBlackboard.CONFIG.set(this.config);

        // Add the launch jar so that our classloader can load classes from it
        SparkLauncher.CLASS_LOADER.addJar(this.config.getLaunchJar());

        try {
            this.plugins.addAll(this.loadPlugins());

            this.plugins.forEach(p -> p.acceptConfig(this.config));
            this.plugins.forEach(p -> p.acceptClassloader(SparkLauncher.CLASS_LOADER));
            this.plugins.forEach(p -> p.volunteerTransformers(SparkLauncher.ROSTER));
        } catch (Throwable t) {
            SparkLauncher.LOGGER.error("An exception occurred while initializing plugins", t);
        }

        LibraryInjector.inject(this.config.getNativeDir());

        // Invoke the given main class with any given arguments

        Class<?> mainClass = SparkLauncher.CLASS_LOADER.loadClass(this.computeMainClass());
        String[] arguments = this.config.getNonOptions().toArray(new String[0]);

        this.plugins.forEach(p -> p.launch(arguments));

        Method main = mainClass.getDeclaredMethod("main", String[].class);
        main.invoke(null, new Object[] { arguments });
    }

    /**
     * Clears out all files in the natives directory
     */
    private void clearNatives() {
        Path nativeDir = this.config.getNativeDir();
        if (Files.exists(nativeDir)) {
            try {
                Files.walk(nativeDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                SparkLauncher.LOGGER.error("Failed to clear natives directory", e);
            }
        }
    }

    /**
     * Loads plugins onto the classpath and then collects them
     *
     * @return all loaded plugins from the classpath
     */
    private Collection<ISparkPlugin> loadPlugins() {
        // TODO: should plugins be loaded on the same classloader as the game?
        Path pluginRoot = SparkLauncher.LAUNCH_DIR.resolve("plugins");
        try {
            this.loadPluginsToClasspath(pluginRoot);
        } catch (IOException e) {
            SparkLauncher.LOGGER.error("Failed to load plugins onto classpath", e);
        }

        return this.collectPlugins();
    }

    /**
     * Collects all plugins from the classpath using a ServiceLoader.
     *
     * @return a collection of collected plugins
     */
    private Collection<ISparkPlugin> collectPlugins() {
        Collection<ISparkPlugin> plugins = new ArrayList<>();

        ServiceLoader<ISparkPlugin> loader = ServiceLoader.load(ISparkPlugin.class, SparkLauncher.CLASS_LOADER);
        Iterator<ISparkPlugin> iterator = loader.iterator();
        while (iterator.hasNext()) {
            try {
                plugins.add(iterator.next());
            } catch (Throwable t) {
                SparkLauncher.LOGGER.error("Failed to load plugin onto classpath", t);
            }
        }

        return plugins;
    }

    /**
     * Loads all plugins from the given root onto the classpath.
     *
     * @param pluginRoot the root directory to scan for plugins
     * @throws IOException if an IO exception occurs while loading plugins
     */
    private void loadPluginsToClasspath(Path pluginRoot) throws IOException {
        if (!Files.exists(pluginRoot)) {
            Files.createDirectory(pluginRoot);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginRoot)) {
            for (Path plugin : stream) {
                if (Files.isDirectory(plugin) || !plugin.toString().endsWith(".jar")) {
                    continue;
                }
                SparkLauncher.CLASS_LOADER.addJar(plugin);
            }
        }
    }

    /**
     * Computes the main class to be used for invocation. If a main class is specified in the config, that will be used.
     * Otherwise, the main class will be loaded from the launch jar manifest.
     *
     * @return the main class fully qualified name to be invoked
     * @throws ClassNotFoundException if a main class is not found
     */
    private String computeMainClass() throws ClassNotFoundException {
        String mainClass = this.config.getMainClass();
        if (mainClass != null) {
            return mainClass;
        }

        String locatedMainClass = this.locateMainClass(this.config.getLaunchJar());
        if (locatedMainClass == null) {
            throw new ClassNotFoundException("No main class provided or found in manifest!");
        }

        return locatedMainClass;
    }

    /**
     * Locates the main class manifest attribute from the given jar file.
     *
     * @param jarFile the jar file to search
     * @return the located main class attribute, or null if none
     */
    private String locateMainClass(Path jarFile) {
        try (JarFile file = new JarFile(jarFile.toFile())) {
            Attributes attributes = file.getManifest().getMainAttributes();
            if (attributes.containsKey(MAIN_CLASS_ATTRIBUTE)) {
                return attributes.getValue(MAIN_CLASS_ATTRIBUTE);
            }
        } catch (IOException e) {
            SparkLauncher.LOGGER.error("Failed to locate main class for {}", jarFile, e);
        }
        return null;
    }
}
