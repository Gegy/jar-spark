package net.gegy1000.bootstrap;

// TODO: write documentation
public class EquilinoxLaunch {
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
