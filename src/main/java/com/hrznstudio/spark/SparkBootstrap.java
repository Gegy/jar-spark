package com.hrznstudio.spark;

import com.hrznstudio.spark.dependency.DependencyExtractor;
import com.hrznstudio.spark.patch.IBytePatcher;
import com.hrznstudio.spark.patch.IPatchPlugin;
import com.hrznstudio.spark.patch.PatchBlackboard;
import com.hrznstudio.spark.plugin.ILaunchPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
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

    private static final Logger LOGGER = LogManager.getLogger("Spark");

    private final BootstrapConfig config;
    private final Collection<ILaunchPlugin> launchPlugins = new ArrayList<>();
    private final Collection<IPatchPlugin> patchPlugins = new ArrayList<>();

    public SparkBootstrap(BootstrapConfig config) {
        this.config = config;
    }

    /**
     * Carries out the launch process given the provided config. This will block until the launch target exits.
     *
     * @throws Throwable if the launch process failed
     */
    public void launch() throws Throwable {
        try {
            PatchBlackboard.CONTEXT.set(SparkLauncher.CLASS_LOADER);

            LOGGER.info("Extracting dependencies from jar");

            this.clearDependencies();
            try (DependencyExtractor dependencyExtractor = DependencyExtractor.open(this.config.getLaunchJar())) {
                dependencyExtractor.extractTo(this.config.getDependencyDir());
            } catch (IOException e) {
                LOGGER.error("Failed to extract dependencies", e);
            }

            PatchBlackboard.key("config").set(this.config);

            // Add the launch jar so that our classloader can load classes from it
            SparkLauncher.CLASS_LOADER.addJar(this.config.getLaunchJar());

            this.injectDependencies();

            try {
                Collection<ClassLoader> pluginClassLoaders = this.loadPlugins();

                this.launchPlugins.addAll(this.collectLaunchPlugins(pluginClassLoaders));
                this.patchPlugins.addAll(this.collectPatchPlugins(pluginClassLoaders));

                LOGGER.info("Loaded {} launch plugins", this.launchPlugins.size());
                LOGGER.info("Loaded {} patch plugins", this.patchPlugins.size());

                this.launchPlugins.forEach(p -> p.acceptConfig(this.config));
                this.launchPlugins.forEach(p -> p.acceptClassloader(SparkLauncher.CLASS_LOADER));

                this.patchPlugins.forEach(IPatchPlugin::initialize);

                for (IPatchPlugin plugin : this.patchPlugins) {
                    for (IBytePatcher patcher : plugin.getPatchers()) {
                        PatcherRoster.INSTANCE.volunteer(patcher);
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Failed to initialize plugins", t);
            }

            // Invoke the given main class with any given arguments

            Class<?> mainClass = SparkLauncher.CLASS_LOADER.loadClass(this.computeMainClass());
            String[] arguments = this.config.getNonOptions().toArray(new String[0]);

            this.launchPlugins.forEach(p -> p.launch(arguments));

            LOGGER.info("Invoking '{}' with arguments: {}", mainClass.getName(), arguments);
            Method main = mainClass.getDeclaredMethod("main", String[].class);
            main.invoke(null, new Object[] { arguments });
        } finally {
            PatchBlackboard.CONTEXT.remove();
        }
    }

    /**
     * Clears out all files in the dependencies directory
     */
    private void clearDependencies() {
        Path dependencyDir = this.config.getDependencyDir();
        if (Files.exists(dependencyDir)) {
            try {
                Files.walk(dependencyDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                LOGGER.error("Failed to clear dependency directory", e);
            }
        }
    }

    private void injectDependencies() throws IOException {
        LOGGER.debug("Injecting jar dependencies");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.config.getDependencyDir())) {
            for (Path dependency : stream) {
                if (Files.isDirectory(dependency) || !dependency.toString().endsWith(".jar")) {
                    continue;
                }
                SparkLauncher.CLASS_LOADER.addJar(dependency);
            }
        }

        LOGGER.debug("Injecting natives");
        LibraryInjector.inject(this.config.getDependencyDir());
    }

    /**
     * Collects all launch plugins from the given plugin classloaders
     *
     * @return all loaded launch plugins
     */
    private Collection<ILaunchPlugin> collectLaunchPlugins(Collection<ClassLoader> classLoaders) {
        Collection<ILaunchPlugin> plugins = new ArrayList<>();
        for (ClassLoader classLoader : classLoaders) {
            ServiceLoader<ILaunchPlugin> loader = ServiceLoader.load(ILaunchPlugin.class, classLoader);
            Iterator<ILaunchPlugin> iterator = loader.iterator();
            while (iterator.hasNext()) {
                try {
                    ILaunchPlugin plugin = iterator.next();
                    plugins.add(plugin);
                    LOGGER.debug("Detected launch plugin '{}'", plugin.getClass().getName());
                } catch (Throwable t) {
                    LOGGER.error("Failed to load launch plugin onto classpath", t);
                }
            }
        }

        return plugins;
    }

    /**
     * Collects all patch plugins from the given plugin classloaders
     *
     * @return all loaded patch plugins
     */
    private Collection<IPatchPlugin> collectPatchPlugins(Collection<ClassLoader> classLoaders) {
        Collection<IPatchPlugin> plugins = new ArrayList<>();
        for (ClassLoader classLoader : classLoaders) {
            ServiceLoader<IPatchPlugin> loader = ServiceLoader.load(IPatchPlugin.class, classLoader);
            Iterator<IPatchPlugin> iterator = loader.iterator();
            while (iterator.hasNext()) {
                try {
                    IPatchPlugin plugin = iterator.next();
                    plugins.add(plugin);
                    LOGGER.debug("Detected patch plugin '{}'", plugin.getClass().getName());
                } catch (Throwable t) {
                    LOGGER.error("Failed to load patch plugin onto classpath", t);
                }
            }
        }

        return plugins;
    }

    private Collection<ClassLoader> loadPlugins() {
        Path pluginRoot = SparkLauncher.LAUNCH_DIR.resolve("plugins");
        Collection<ClassLoader> plugins = new ArrayList<>();

        try {
            Collection<URL> pluginJars = this.collectPluginJars(pluginRoot);
            for (URL pluginUrl : pluginJars) {
                plugins.add(new URLClassLoader(new URL[] { pluginUrl }, this.getClass().getClassLoader()));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load plugins from files", e);
        }

        // If we are in a plugin dev env, we want to find plugins on the launch classpath
        plugins.add(this.getClass().getClassLoader());

        return plugins;
    }

    private Collection<URL> collectPluginJars(Path pluginRoot) throws IOException {
        if (!Files.exists(pluginRoot)) {
            Files.createDirectory(pluginRoot);
        }

        Collection<URL> pluginJars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginRoot)) {
            for (Path plugin : stream) {
                if (Files.isDirectory(plugin) || !plugin.toString().endsWith(".jar")) {
                    continue;
                }
                pluginJars.add(plugin.toUri().toURL());
            }
        }

        return pluginJars;
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
            LOGGER.error("Failed to locate main class for {}", jarFile, e);
        }
        return null;
    }
}
