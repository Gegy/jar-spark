package com.hrznstudio.spark;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Represents user config built from command line input
 */
public class BootstrapConfig {
    private final OptionSet optionSet;
    private final OptionSpec<Path> launchJar;
    private final OptionSpec<Path> dependencyDir;
    private final OptionSpec<String> mainClass;
    private final OptionSpec<String> nonOption;

    private BootstrapConfig(
            OptionSet optionSet,
            OptionSpec<Path> launchJar,
            OptionSpec<Path> dependencyDir,
            OptionSpec<String> mainClass,
            OptionSpec<String> nonOption
    ) {
        this.optionSet = optionSet;
        this.launchJar = launchJar;
        this.dependencyDir = dependencyDir;
        this.mainClass = mainClass;
        this.nonOption = nonOption;
    }

    /**
     * Parses a config from a raw command line argument array
     *
     * @param arguments raw command line arguments to parse
     * @return a parsed config
     */
    public static BootstrapConfig parse(String... arguments) {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        OptionSpec<Path> launchJar = parser.accepts("launchJar", "The jar to launch after bootstrap")
                .withOptionalArg()
                .withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING));

        OptionSpec<Path> nativeDir = parser.accepts("dependencyDir", "The directory for dependencies to be extracted to")
                .withOptionalArg()
                .withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING));

        OptionSpec<String> mainClass = parser.accepts("mainClass", "The fully qualified main class name to be invoked")
                .withOptionalArg();

        OptionSpec<String> nonOption = parser.nonOptions();

        OptionSet optionSet = parser.parse(arguments);
        return new BootstrapConfig(optionSet, launchJar, nativeDir, mainClass, nonOption);
    }

    /**
     * Extracts the launch jar from this config. The launch jar is that which will be bootstrapped and transformed
     * by any loaded plugins.
     *
     * If no launch jar is specified, this will default to the current jar
     *
     * @return the jar to be launched
     */
    public Path getLaunchJar() {
        Path launchJar = this.launchJar.value(this.optionSet);
        if (launchJar == null) {
            try {
                return Paths.get(SparkLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Current jar file is invalid, cannot launch!", e);
            }
        }
        return launchJar;
    }

    /**
     * Extracts the dependency directory from this config. The dependency directory will hold all dependencies extracted
     * from the launch jar at startup.
     *
     * If no dependency directory is specified, this will default to /dependencies
     *
     * @return the dependency directory
     */
    public Path getDependencyDir() {
        Path dependencies = this.dependencyDir.value(this.optionSet);
        if (dependencies == null) {
            return SparkLauncher.LAUNCH_DIR.resolve("dependencies");
        }
        return dependencies;
    }

    /**
     * Extracts the main class from this config. The main class is that which will be invoked when launching the launch
     * jar.
     *
     * If null is returned, the main class attribute in the launch jar attribute should be invoked.
     *
     * @return the provided name class, or null if none given
     */
    public String getMainClass() {
        return this.mainClass.value(this.optionSet);
    }

    /**
     * Extracts all non-options from this config. These will be parsed as regular arguments to the target jar. These
     * are specified following a padded '--'
     *
     * @return a list of non-option arguments
     */
    public List<String> getNonOptions() {
        return this.nonOption.values(this.optionSet);
    }
}
