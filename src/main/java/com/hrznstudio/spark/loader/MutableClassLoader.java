package com.hrznstudio.spark.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class MutableClassLoader extends URLClassLoader {
    private static final Logger LOGGER = LogManager.getLogger("ClassLoader");

    public MutableClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        LOGGER.debug("Adding {} to classpath", url);
        super.addURL(url);
    }

    public void addJar(Path path) throws IOException {
        this.addURL(path.toUri().toURL());
    }
}
