package net.gegy1000.bootstrap.natives;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NativeExtractor implements AutoCloseable {
    private final NativeCollector collector;

    private NativeExtractor(NativeCollector collector) {
        this.collector = collector;
    }

    public static NativeExtractor open(Path jarPath) throws IOException {
        return new NativeExtractor(NativeCollector.open(jarPath));
    }

    public void extractTo(Path path) throws IOException {
        for (PackagedNative packagedNative : this.collector) {
            this.extractNative(path, packagedNative);
        }
    }

    private void extractNative(Path root, PackagedNative packagedNative) throws IOException {
        try (InputStream input = packagedNative.getInputStream()) {
            Path extractedPath = root.resolve(Paths.get(packagedNative.getName()));
            Files.copy(input, extractedPath);
        }
    }

    @Override
    public void close() throws Exception {
        this.collector.close();
    }
}
