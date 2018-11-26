package net.gegy1000.bootstrap.loader;

import net.gegy1000.bootstrap.EquilinoxLaunch;
import net.gegy1000.bootstrap.transformer.IByteTransformer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TransformingClassLoader extends URLClassLoader {
    public TransformingClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // TODO: Filter
        try {
            URLConnection connection = this.openClassConnection(name);
            CodeSource source = this.getSource(name, connection);
            byte[] bytes = this.readClassBytes(connection);
            byte[] transformedBytes = this.transformClass(name, bytes);
            return this.defineClass(name, transformedBytes, 0, transformedBytes.length, source);
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to read class", e);
        }
    }

    private byte[] transformClass(String target, byte[] input) {
        Collection<IByteTransformer> transformers = EquilinoxLaunch.ROSTER.collectVolunteers(target);
        if (transformers.isEmpty()) {
            return input;
        }

        byte[] bytes = input;
        for (IByteTransformer transformer : transformers) {
            bytes = transformer.transform(target, bytes);
        }

        return bytes;
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
        return this.findResource(path).openConnection();
    }

    private CodeSource getSource(String name, URLConnection connection) {
        if (connection instanceof JarURLConnection) {
            try {
                JarURLConnection jarConnection = (JarURLConnection) connection;
                JarFile jar = jarConnection.getJarFile();
                JarEntry entry = jar.getJarEntry(toPath(name));
                return new CodeSource(jarConnection.getJarFileURL(), entry.getCodeSigners());
            } catch (IOException e) {
                // TODO: Logger
            }
        }
        return new CodeSource(connection.getURL(), new CodeSigner[0]);
    }

    private static String toPath(String name) {
        return name.replace('.', '/') + ".class";
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
