package com.hrznstudio.spark;

import com.hrznstudio.spark.loader.MutableClassLoader;
import com.hrznstudio.spark.loader.TransformingClassLoader;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: write documentation

/**
 * Entry-point to the spark launcher. This parses command line arguments into a config and sends it to the bootstrapper
 * to handle.
 */
public class SparkLauncher {
    public static final TransformingClassLoader CLASS_LOADER = new TransformingClassLoader(SparkLauncher.class.getClassLoader());
    public static final MutableClassLoader PLUGIN_CLASS_LOADER = new MutableClassLoader(new URL[0], SparkLauncher.class.getClassLoader());

    public static final Path LAUNCH_DIR = Paths.get("");

    public static void main(String[] args) throws Throwable {
        Thread.currentThread().setContextClassLoader(SparkLauncher.CLASS_LOADER);

        BootstrapConfig config = BootstrapConfig.parse(args);
        SparkBootstrap bootstrap = new SparkBootstrap(config);

        bootstrap.launch();
    }
}
