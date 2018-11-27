package net.gegy1000.spark.plugin;

import net.gegy1000.spark.BootstrapConfig;
import net.gegy1000.spark.loader.TransformingClassLoader;
import net.gegy1000.spark.transformer.TransformerRoster;

/**
 * A plugin to be loaded before game startup, allowing for bytecode transformers to be registered
 */
public interface IBootstrapPlugin {
    void acceptConfig(BootstrapConfig config);

    void acceptClassloader(TransformingClassLoader classLoader);

    /**
     * Called before game startup. All transformers should be registered here.
     *
     * @param roster the roster to register transformers to
     */
    void volunteerTransformers(TransformerRoster roster);

    void launch(String[] arguments);
}
