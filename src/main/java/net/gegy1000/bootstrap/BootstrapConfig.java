package net.gegy1000.bootstrap;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BootstrapConfig {
    private final OptionSet optionSet;
    private final OptionSpec<Path> launchJar;
    private final OptionSpec<Path> nativeDir;
    private final OptionSpec<String> mainClass;

    private BootstrapConfig(
            OptionSet optionSet,
            OptionSpec<Path> launchJar,
            OptionSpec<Path> nativeDir,
            OptionSpec<String> mainClass
    ) {
        this.optionSet = optionSet;
        this.launchJar = launchJar;
        this.nativeDir = nativeDir;
        this.mainClass = mainClass;
    }

    public static BootstrapConfig parse(String... arguments) {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        OptionSpec<Path> launchJar = parser.accepts("launchJar", "The jar to launch after bootstrap")
                .withOptionalArg()
                .withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING));

        OptionSpec<Path> nativeDir = parser.accepts("nativeDir", "The directory for natives to be extracted to")
                .withOptionalArg()
                .withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING));

        OptionSpec<String> mainClass = parser.accepts("mainClass", "The fully qualified main class name to be invoked")
                .withOptionalArg();

        OptionSet optionSet = parser.parse(arguments);
        return new BootstrapConfig(optionSet, launchJar, nativeDir, mainClass);
    }

    public Path getLaunchJar() {
        Path launchJar = this.launchJar.value(this.optionSet);
        if (launchJar == null) {
            try {
                return Paths.get(EquilinoxLaunch.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Current jar file was invalid, cannot launch!");
            }
        }
        return launchJar;
    }

    public Path getNativeDir() {
        Path natives = this.nativeDir.value(this.optionSet);
        if (natives == null) {
            return EquilinoxLaunch.LAUNCH_DIR.resolve("natives");
        }
        return natives;
    }

    public String getMainClass() {
        return this.mainClass.value(this.optionSet);
    }
}
