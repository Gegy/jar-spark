package com.hrznstudio.spark.plugin;

import com.hrznstudio.spark.BootstrapConfig;
import com.hrznstudio.spark.loader.TransformingClassLoader;

/**
 * A plugin to be loaded before game startup
 */
public interface ILaunchPlugin {
    void acceptConfig(BootstrapConfig config);

    void acceptClassloader(TransformingClassLoader classLoader);

    void launch(String[] arguments);
}
