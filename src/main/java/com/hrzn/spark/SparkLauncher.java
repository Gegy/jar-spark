package com.hrzn.spark;

import com.hrzn.spark.loader.TransformingClassLoader;
import com.hrzn.spark.transformer.TransformerRoster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: write documentation
// TODO: support jar-in-jar

/**
 * Entry-point to the spark launcher. This parses command line arguments into a config and sends it to the bootstrapper
 * to handle.
 */
public class SparkLauncher {
    public static final TransformingClassLoader CLASS_LOADER = new TransformingClassLoader(SparkLauncher.class.getClassLoader());
    public static final TransformerRoster ROSTER = new TransformerRoster();

    public static final Path LAUNCH_DIR = Paths.get("");

    public static final Logger LOGGER = LogManager.getLogger("Spark");

    public static void main(String[] args) throws Throwable {
        BootstrapConfig config = BootstrapConfig.parse(args);
        SparkBootstrap bootstrap = new SparkBootstrap(config);

        bootstrap.launch();
    }
}
