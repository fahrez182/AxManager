package rikka.shizuku.manager;

import rikka.shizuku.server.ClientManager;

public class ShizukuClientManager extends ClientManager<ShizukuConfigManager> {

    public ShizukuClientManager(ShizukuConfigManager configManager) {
        super(configManager);
    }
}


