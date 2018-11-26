package net.gegy1000.bootstrap;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;

public class LibraryInjector {
    private static final String LIBRARY_PATH_ENV = "java.library.path";

    public static void inject(Path library) {
        String path = library.toAbsolutePath().toString();

        String property = System.getProperty(LIBRARY_PATH_ENV);
        System.setProperty(LIBRARY_PATH_ENV, path + File.pathSeparator + property);

        invalidatePaths();
    }

    private static void invalidatePaths() {
        try {
            Field sysPaths = ClassLoader.class.getDeclaredField("sys_paths");
            sysPaths.setAccessible(true);
            sysPaths.set(null, null);
        } catch (ReflectiveOperationException e) {
            // TODO: Logger
        }
    }
}
