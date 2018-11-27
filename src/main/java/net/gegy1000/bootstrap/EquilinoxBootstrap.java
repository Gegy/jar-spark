package net.gegy1000.bootstrap;

import net.gegy1000.bootstrap.natives.NativeExtractor;
import net.gegy1000.bootstrap.plugin.IBootstrapPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Given a config, this carries out the bootstrap process: extracting natives, loading plugins, injecting natives,
 * initializing the classloader and invoking the launch jar.
 */
public class EquilinoxBootstrap {
    private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

    private final BootstrapConfig config;

    public EquilinoxBootstrap(BootstrapConfig config) {
        this.config = config;
    }

    /**
     * Carries out the launch process given the provided config. This will block until the launch target exits.
     *
     * @throws Exception if the launch process failed
     */
    public void launch() throws Exception {
        Thread.currentThread().setContextClassLoader(EquilinoxLaunch.CLASS_LOADER);

        this.clearNatives();
        try (NativeExtractor nativeExtractor = NativeExtractor.open(this.config.getLaunchJar())) {
            nativeExtractor.extractTo(this.config.getNativeDir());
        }

        // Add the launch jar so that our classloader can load classes from it
        EquilinoxLaunch.CLASS_LOADER.addJar(this.config.getLaunchJar());

        this.loadPlugins();

        LibraryInjector.inject(this.config.getNativeDir());

        // Invoke the given main class with any given arguments

        Class<?> mainClass = EquilinoxLaunch.CLASS_LOADER.loadClass(this.computeMainClass());
        String[] arguments = this.config.getNonOptions().toArray(new String[0]);

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
                // TODO
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads plugins onto the classpath and registers them to the TransformerRoster
     */
    private void loadPlugins() {
        Path pluginRoot = EquilinoxLaunch.LAUNCH_DIR.resolve("plugins");
        try {
            this.loadPluginsToClasspath(pluginRoot);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        Collection<IBootstrapPlugin> plugins = this.collectPlugins();
        for (IBootstrapPlugin plugin : plugins) {
            plugin.volunteerTransformers(EquilinoxLaunch.ROSTER);
        }
    }

    /**
     * Collects all plugins from the classpath using a ServiceLoader.
     *
     * @return a collection of collected plugins
     */
    private Collection<IBootstrapPlugin> collectPlugins() {
        Collection<IBootstrapPlugin> plugins = new ArrayList<>();

        ServiceLoader<IBootstrapPlugin> loader = ServiceLoader.load(IBootstrapPlugin.class, EquilinoxLaunch.CLASS_LOADER);
        for (IBootstrapPlugin plugin : loader) {
            plugins.add(plugin);
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
                EquilinoxLaunch.CLASS_LOADER.addJar(plugin);
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
            // TODO: Log
        }
        return null;
    }
}
