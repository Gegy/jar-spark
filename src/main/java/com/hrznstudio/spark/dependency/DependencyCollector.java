package com.hrznstudio.spark.dependency;

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

public class DependencyCollector implements Iterable<PackagedDependency>, AutoCloseable {
    private final JarFile jarFile;
    private final Collection<PackagedDependency> entries;

    private DependencyCollector(JarFile jarFile, Collection<PackagedDependency> entries) {
        this.jarFile = jarFile;
        this.entries = entries;
    }

    public static DependencyCollector open(Path jarPath) throws IOException {
        Predicate<JarEntry> predicate = buildDependencyPredicate(System.getProperty("os.name"));
        return DependencyCollector.open(jarPath, predicate);
    }

    public static DependencyCollector open(Path jarPath, Predicate<JarEntry> nativePredicate) throws IOException {
        JarFile jarFile = new JarFile(jarPath.toFile());
        Enumeration<JarEntry> entries = jarFile.entries();
        Collection<PackagedDependency> nativeEntries = new ArrayList<>();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (nativePredicate.test(entry)) {
                nativeEntries.add(new PackagedDependency(jarFile, entry));
            }
        }

        return new DependencyCollector(jarFile, nativeEntries);
    }

    public static Predicate<JarEntry> buildDependencyPredicate(String os) {
        String osNormalized = os.toLowerCase(Locale.ROOT);

        boolean windows = osNormalized.startsWith("win");
        boolean linux = osNormalized.startsWith("linux");
        boolean mac = osNormalized.startsWith("mac") || osNormalized.startsWith("darwin");

        return entry -> {
            if (entry.isDirectory()) {
                return false;
            }

            String name = entry.getName();
            return name.endsWith(".jar")
                    || windows && name.endsWith(".dll")
                    || (linux && name.endsWith(".so"))
                    || mac && (name.endsWith(".dylib") || name.endsWith(".jnilib"));
        };
    }

    @Override
    public Iterator<PackagedDependency> iterator() {
        return this.entries.iterator();
    }

    @Override
    public void close() throws IOException {
        this.jarFile.close();
    }
}
