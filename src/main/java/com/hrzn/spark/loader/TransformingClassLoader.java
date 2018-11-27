package com.hrzn.spark.loader;

import com.hrzn.spark.transformer.IByteTransformer;
import com.hrzn.spark.SparkLauncher;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

// TODO: review unused utils
// TODO: default exclusions
public class TransformingClassLoader extends URLClassLoader {
    private final Map<String, Throwable> invalidClasses = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    private final Set<String> loadExemptions = new HashSet<>();
    private final Set<String> transformExemptions = new HashSet<>();

    public TransformingClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        this.ensureValid(name);

        if (this.isExempt(name, this.loadExemptions)) {
            try {
                return super.findClass(name);
            } catch (Throwable t) {
                this.invalidClasses.put(name, t);
                throw new ClassNotFoundException("Failed to load exempt class " + name, t);
            }
        }

        Class<?> loadedClass = this.loadedClasses.get(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            Class<?> clazz = this.readClass(name);
            this.loadedClasses.put(name, clazz);

            return clazz;
        } catch (Throwable t) {
            this.invalidClasses.put(name, t);
            throw new ClassNotFoundException("Failed to load class " + name, t);
        }
    }

    private Class<?> readClass(String name) throws Throwable {
        URLConnection connection = this.openClassConnection(name);
        JarContext context = this.getJarContext(name, connection);
        if (context == null) {
            throw new ClassNotFoundException("Could not get jar context for class " + name);
        }

        CodeSource source = context.buildSource();
        Manifest manifest = context.manifest;

        int lastPackageSplit = name.lastIndexOf('.');
        if (lastPackageSplit != -1) {
            String packageName = name.substring(0, lastPackageSplit);
            if (this.getPackage(packageName) == null) {
                this.definePackage(packageName, manifest, context.jarUrl);
            }
        }

        byte[] bytes = this.readClassBytes(connection);
        byte[] transformedBytes = this.transformClass(name, bytes);
        return this.defineClass(name, transformedBytes, 0, transformedBytes.length, source);
    }

    private void ensureValid(String name) throws ClassNotFoundException {
        Throwable throwable = this.invalidClasses.get(name);
        if (throwable != null) {
            throw new ClassNotFoundException("Failed to load class", throwable);
        }
    }

    private boolean isExempt(String name, Set<String> prefixes) {
        return prefixes.stream().anyMatch(name::startsWith);
    }

    private byte[] transformClass(String target, byte[] input) {
        if (this.isExempt(target, this.transformExemptions)) {
            return input;
        }

        Collection<IByteTransformer> transformers = SparkLauncher.ROSTER.getTransformers();

        byte[] bytes = input;
        for (IByteTransformer transformer : transformers) {
            bytes = transformer.transform(target, bytes);
        }

        return bytes;
    }

    public byte[] readClassBytes(String name) throws IOException {
        // TODO: cache?
        URLConnection connection = this.openClassConnection(name);
        if (connection == null) {
            return null;
        }
        return this.readClassBytes(connection);
    }

    private byte[] readClassBytes(URLConnection connection) throws IOException {
        try (DataInputStream input = new DataInputStream(connection.getInputStream())) {
            byte[] bytes = new byte[connection.getContentLength()];
            input.readFully(bytes);
            return bytes;
        }
    }

    private URLConnection openClassConnection(String name) throws IOException {
        String path = toPath(name);
        URL resource = this.findResource(path);
        if (resource == null) {
            return null;
        }
        return resource.openConnection();
    }

    private JarContext getJarContext(String name, URLConnection connection) {
        if (connection instanceof JarURLConnection) {
            try {
                JarURLConnection jarConnection = (JarURLConnection) connection;
                JarFile jar = jarConnection.getJarFile();
                JarEntry entry = jar.getJarEntry(toPath(name));
                return new JarContext(jarConnection.getJarFileURL(), entry.getCodeSigners(), jar.getManifest());
            } catch (IOException e) {
                SparkLauncher.LOGGER.error("Failed to load jar context for {}", name, e);
            }
        }
        return null;
    }

    private static String toPath(String name) {
        return name.replace('.', '/') + ".class";
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public void addJar(Path path) throws IOException {
        this.addURL(path.toUri().toURL());
    }

    public void invalidate(String name, Throwable t) {
        this.invalidClasses.put(name, t);
    }

    public void addLoadExemption(String prefix) {
        this.loadExemptions.add(prefix);
    }

    public void addTransformExemption(String prefix) {
        this.transformExemptions.add(prefix);
    }

    public boolean isLoadExempt(String name) {
        return this.isExempt(name, this.loadExemptions);
    }

    public boolean isTransformExempt(String name) {
        return this.isExempt(name, this.transformExemptions);
    }

    public boolean isLoaded(String name) {
        return this.loadedClasses.containsKey(name);
    }

    public boolean isInvalid(String name) {
        return this.invalidClasses.containsKey(name);
    }

    private static class JarContext {
        private final URL jarUrl;
        private final CodeSigner[] signers;
        private final Manifest manifest;

        private JarContext(URL jarUrl, CodeSigner[] signers, Manifest manifest) {
            this.jarUrl = jarUrl;
            this.signers = signers;
            this.manifest = manifest;
        }

        CodeSource buildSource() {
            return new CodeSource(this.jarUrl, this.signers);
        }
    }
}
