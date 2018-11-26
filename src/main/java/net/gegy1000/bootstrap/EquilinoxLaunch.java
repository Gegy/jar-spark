package net.gegy1000.bootstrap;

import net.gegy1000.bootstrap.loader.TransformingClassLoader;
import net.gegy1000.bootstrap.transformer.TransformerRoster;

// TODO: write documentation
public class EquilinoxLaunch {
    public static final TransformingClassLoader CLASS_LOADER = new TransformingClassLoader(EquilinoxLaunch.class.getClassLoader());
    public static final TransformerRoster ROSTER = new TransformerRoster();

    public static void main(String[] args) {
        BootstrapConfig config = BootstrapConfig.parse(args);
        EquilinoxBootstrap bootstrap = new EquilinoxBootstrap(config);

        // TODO: not excepting if missing args
        try {
            bootstrap.launch();
        } catch (Exception e) {
            // TODO: log
        } finally {
            bootstrap.cleanUp();
        }
    }
}
