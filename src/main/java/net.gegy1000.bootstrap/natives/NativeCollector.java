package net.gegy1000.bootstrap.natives;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeCollector implements Iterable<PackagedNative>, AutoCloseable {
    private final JarFile jarFile;
    private final Collection<PackagedNative> entries;

    private NativeCollector(JarFile jarFile, Collection<PackagedNative> entries) {
        this.jarFile = jarFile;
        this.entries = entries;
    }

    public static NativeCollector open(Path jarPath) throws IOException {
        Predicate<JarEntry> predicate = buildNativePredicate(System.getProperty("os.name"));
        return NativeCollector.open(jarPath, predicate);
    }

    public static NativeCollector open(Path jarPath, Predicate<JarEntry> nativePredicate) throws IOException {
        JarFile jarFile = new JarFile(jarPath.toFile());
        Enumeration<JarEntry> entries = jarFile.entries();
        Collection<PackagedNative> nativeEntries = new ArrayList<>();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (nativePredicate.test(entry)) {
                nativeEntries.add(new PackagedNative(jarFile, entry));
            }
        }

        return new NativeCollector(jarFile, nativeEntries);
    }

    public static Predicate<JarEntry> buildNativePredicate(String os) {
        String osNormalized = os.toLowerCase(Locale.ROOT);

        boolean windows = osNormalized.startsWith("win");
        boolean linux = osNormalized.startsWith("linux");
        boolean mac = osNormalized.startsWith("mac") || osNormalized.startsWith("darwin");

        return entry -> {
            if (entry.isDirectory()) {
                return false;
            }

            String name = entry.getName();
            return windows && name.endsWith(".dll")
                    || (linux && name.endsWith(".so"))
                    || mac && (name.endsWith(".dylib") || name.endsWith(".jnilib"));
        };
    }

    @Override
    public Iterator<PackagedNative> iterator() {
        return this.entries.iterator();
    }

    @Override
    public void close() throws IOException {
        this.jarFile.close();
    }
}
