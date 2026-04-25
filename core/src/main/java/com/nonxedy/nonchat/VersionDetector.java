public class VersionDetector {

    public static IPlatformAdapter detect() {
        String version = Bukkit.getBukkitVersion(); // e.g. "1.21.1-R0.1-SNAPSHOT"
        
        // ServiceLoader находит реализацию из classpath (нужный адаптер уже в jar)
        ServiceLoader<IPlatformAdapter> loader =
            ServiceLoader.load(IPlatformAdapter.class);

        for (IPlatformAdapter adapter : loader) {
            if (version.startsWith(adapter.getSupportedVersion())) {
                return adapter;
            }
        }
        throw new IllegalStateException("Unsupported server version: " + version);
    }
}