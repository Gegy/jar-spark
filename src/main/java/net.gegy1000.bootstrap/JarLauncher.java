package net.gegy1000.bootstrap;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class JarLauncher {
    private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

    private final Path path;

    private String mainClass;

    private final List<String> arguments = new ArrayList<>();

    public JarLauncher(Path path) {
        this.path = path;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void addArguments(String... arguments) {
        Collections.addAll(this.arguments, arguments);
    }

    public void launch(ClassLoader classLoader) throws ReflectiveOperationException {
        String[] arguments = this.arguments.toArray(new String[0]);

        Class<?> mainClass = classLoader.loadClass(this.computeMainClass());

        Method main = mainClass.getDeclaredMethod("main", String[].class);
        main.invoke(null, new Object[] { arguments });
    }

    private String computeMainClass() throws ClassNotFoundException {
        if (this.mainClass != null) {
            return this.mainClass;
        }

        String mainClass = this.locateMainClass();
        if (mainClass == null) {
            throw new ClassNotFoundException("No main class provided or found in manifest!");
        }

        return mainClass;
    }

    private String locateMainClass() {
        try (JarFile file = new JarFile(this.path.toFile())) {
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
