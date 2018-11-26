package net.gegy1000.bootstrap.loader;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TransformingClassLoader extends URLClassLoader {
    public TransformingClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // TODO: Filter & transform
        try {
            URLConnection connection = this.openClassConnection(name);
            CodeSource source = this.getSource(name, connection);
            byte[] bytes = this.readClassBytes(connection);
            return this.defineClass(name, bytes, 0, bytes.length, source);
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to read class", e);
        }
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
}
