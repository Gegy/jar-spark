package com.hrzn.spark;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * A util allowing libraries to be injected into the java library path at runtime.
 */
public class LibraryInjector {
    private static final String LIBRARY_PATH_ENV = "java.library.path";

    /**
     * Injects the given path as a library into the environment
     *
     * @param library the library path to inject
     */
    public static void inject(Path library) {
        String path = library.toAbsolutePath().toString();

        String property = System.getProperty(LIBRARY_PATH_ENV);
        System.setProperty(LIBRARY_PATH_ENV, path + File.pathSeparator + property);

        invalidatePaths();
    }

    /**
     * Invalidates the JVM cache of library paths, allowing the injected libraries to become effective
     */
    private static void invalidatePaths() {
        try {
            Field sysPaths = ClassLoader.class.getDeclaredField("sys_paths");
            sysPaths.setAccessible(true);
            sysPaths.set(null, null);
        } catch (ReflectiveOperationException e) {
            SparkLauncher.LOGGER.error("Failed to invalidate library path", e);
        }
    }
}
