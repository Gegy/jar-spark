package net.gegy1000.bootstrap;

import net.gegy1000.bootstrap.natives.NativeExtractor;
import net.gegy1000.bootstrap.plugin.IBootstrapPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ServiceLoader;

public class EquilinoxBootstrap {
    private final BootstrapConfig config;

    public EquilinoxBootstrap(BootstrapConfig config) {
        this.config = config;
    }

    public void launch() throws Exception {
        Thread.currentThread().setContextClassLoader(EquilinoxLaunch.CLASS_LOADER);

        try (NativeExtractor nativeExtractor = NativeExtractor.open(this.config.getLaunchJar())) {
            nativeExtractor.extractTo(this.config.getNativeDir());
        }

        URL launchJarUrl = this.config.getLaunchJar().toUri().toURL();
        EquilinoxLaunch.CLASS_LOADER.addURL(launchJarUrl);

        Collection<IBootstrapPlugin> plugins = this.collectPlugins();
        for (IBootstrapPlugin plugin : plugins) {
            plugin.volunteerTransformers(EquilinoxLaunch.ROSTER);
        }

        LibraryInjector.inject(this.config.getNativeDir());

        JarLauncher launcher = new JarLauncher(this.config.getLaunchJar());

        String mainClass = this.config.getMainClass();
        if (mainClass != null) {
            launcher.setMainClass(mainClass);
        }

        launcher.launch(EquilinoxLaunch.CLASS_LOADER);
    }

    public void cleanUp() {
        try {
            Files.walk(this.config.getNativeDir())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    private Collection<IBootstrapPlugin> collectPlugins() {
        Path pluginRoot = this.config.getLaunchDir().resolve("plugins");

        Collection<IBootstrapPlugin> plugins = new ArrayList<>();

        try {
            this.loadPluginsToClasspath(pluginRoot);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        ServiceLoader<IBootstrapPlugin> loader = ServiceLoader.load(IBootstrapPlugin.class, EquilinoxLaunch.CLASS_LOADER);
        for (IBootstrapPlugin plugin : loader) {
            plugins.add(plugin);
        }

        return plugins;
    }

    private void loadPluginsToClasspath(Path pluginRoot) throws IOException {
        if (!Files.exists(pluginRoot)) {
            Files.createDirectory(pluginRoot);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginRoot)) {
            for (Path plugin : stream) {
                if (Files.isDirectory(plugin)) {
                    continue;
                }
                URL url = plugin.toUri().toURL();
                EquilinoxLaunch.CLASS_LOADER.addURL(url);
            }
        }
    }
}
