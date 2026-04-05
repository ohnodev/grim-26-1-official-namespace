package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.updates.GrimUpdateCheckService;

public class UpdateChecker implements StartableInitable {
    @Override
    public void start() {
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            GrimUpdateCheckService.checkForUpdatesAsync(GrimAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
