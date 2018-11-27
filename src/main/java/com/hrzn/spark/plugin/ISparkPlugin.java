package com.hrzn.spark.plugin;

import com.hrzn.spark.BootstrapConfig;
import com.hrzn.spark.loader.TransformingClassLoader;
import com.hrzn.spark.transformer.TransformerRoster;

/**
 * A plugin to be loaded before game startup, allowing for bytecode transformers to be registered
 */
public interface ISparkPlugin {
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
