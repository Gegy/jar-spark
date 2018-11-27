package net.gegy1000.spark;

import net.gegy1000.spark.loader.TransformingClassLoader;
import net.gegy1000.spark.transformer.TransformerRoster;

import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: write documentation

/**
 * Entry-point to the spark launcher. This parses command line arguments into a config and sends it to the bootstrapper
 * to handle.
 */
public class SparkLauncher {
    public static final TransformingClassLoader CLASS_LOADER = new TransformingClassLoader(SparkLauncher.class.getClassLoader());
    public static final TransformerRoster ROSTER = new TransformerRoster();

    public static final Path LAUNCH_DIR = Paths.get("");

    public static void main(String[] args) {
        BootstrapConfig config = BootstrapConfig.parse(args);
        SparkBootstrap bootstrap = new SparkBootstrap(config);

        try {
            bootstrap.launch();
        } catch (Exception e) {
            // TODO: log
        }
    }
}
