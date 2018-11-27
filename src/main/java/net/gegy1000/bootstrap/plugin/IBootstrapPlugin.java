package net.gegy1000.bootstrap.plugin;

import net.gegy1000.bootstrap.transformer.TransformerRoster;

/**
 * A plugin to be loaded before game startup, allowing for bytecode transformers to be registered
 */
public interface IBootstrapPlugin {
    /**
     * Called before game startup. All transformers should be registered here.
     *
     * @param roster the roster to register transformers to
     */
    void volunteerTransformers(TransformerRoster roster);
}
