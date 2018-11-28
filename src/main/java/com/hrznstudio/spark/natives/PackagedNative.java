package com.hrznstudio.spark.natives;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackagedNative {
    private final JarFile jarFile;
    private final JarEntry entry;

    public PackagedNative(JarFile jarFile, JarEntry entry) {
        this.jarFile = jarFile;
        this.entry = entry;
    }

    public String getName() {
        return this.entry.getName();
    }

    public InputStream getInputStream() throws IOException {
        return this.jarFile.getInputStream(this.entry);
    }
}
