package com.hrznstudio.spark.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DependencyExtractor implements AutoCloseable {
    private final DependencyCollector collector;

    private DependencyExtractor(DependencyCollector collector) {
        this.collector = collector;
    }

    public static DependencyExtractor open(Path jarPath) throws IOException {
        return new DependencyExtractor(DependencyCollector.open(jarPath));
    }

    public void extractTo(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }

        for (PackagedDependency packagedDependency : this.collector) {
            this.extractDependency(path, packagedDependency);
        }
    }

    private void extractDependency(Path root, PackagedDependency packagedDependency) throws IOException {
        try (InputStream input = packagedDependency.getInputStream()) {
            Path extractedPath = root.resolve(Paths.get(packagedDependency.getName()));
            Files.copy(input, extractedPath);
        }
    }

    @Override
    public void close() throws Exception {
        this.collector.close();
    }
}
