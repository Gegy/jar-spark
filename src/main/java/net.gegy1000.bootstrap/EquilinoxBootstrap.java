package net.gegy1000.bootstrap;

import net.gegy1000.bootstrap.loader.TransformingClassLoader;
import net.gegy1000.bootstrap.natives.NativeExtractor;
import net.gegy1000.bootstrap.plugin.IBootstrapPlugin;

import java.io.File;
import java.net.URL;
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
        try (NativeExtractor nativeExtractor = NativeExtractor.open(this.config.getLaunchJar())) {
            nativeExtractor.extractTo(this.config.getNativeDir());
        }

        ClassLoader parent = EquilinoxBootstrap.class.getClassLoader();
        URL launchJarUrl = this.config.getLaunchJar().toUri().toURL();
        ClassLoader classLoader = new TransformingClassLoader(new URL[] { launchJarUrl }, parent);

        // TODO: static classloader?
        Thread.currentThread().setContextClassLoader(classLoader);

        LibraryInjector.inject(this.config.getNativeDir());

        JarLauncher launcher = new JarLauncher(this.config.getLaunchJar());

        String mainClass = this.config.getMainClass();
        if (mainClass != null) {
            launcher.setMainClass(mainClass);
        }

        launcher.launch(classLoader);
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
        Collection<IBootstrapPlugin> plugins = new ArrayList<>();

        // TODO: Scan in files
        ServiceLoader<IBootstrapPlugin> loader = ServiceLoader.load(IBootstrapPlugin.class);
        for (IBootstrapPlugin plugin : loader) {
            plugins.add(plugin);
        }

        return plugins;
    }
}
