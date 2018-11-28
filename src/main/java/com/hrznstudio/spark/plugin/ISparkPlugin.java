package com.hrznstudio.spark.plugin;

import com.hrznstudio.spark.BootstrapConfig;
import com.hrznstudio.spark.loader.TransformingClassLoader;
import com.hrznstudio.spark.transformer.TransformerRoster;

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
