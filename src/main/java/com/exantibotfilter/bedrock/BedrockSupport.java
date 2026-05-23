package com.exantibotfilter.bedrock;

import com.exantibotfilter.ExAntiBotFilter;
import com.exantibotfilter.captcha.CaptchaSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Floodgate yuklangan bo'lsa Bedrock playerlarga Form yuborishni tashkil qiladi.
 * Floodgate yo'q bo'lsa, hech narsa qilmaydi - Java menyusi ishlatiladi.
 *
 * Floodgate API to'g'ridan-to'g'ri faqat {@link FloodgateBridge} classida ishlatiladi.
 * Shu sababli plugin Floodgate'siz serverlarda ham xatosiz yuklanadi.
 */
public class BedrockSupport {

    private final ExAntiBotFilter plugin;
    private boolean floodgateLoaded = false;
    private FloodgateBridge bridge;

    public BedrockSupport(ExAntiBotFilter plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            // Floodgate plugini ro'yxatdan tekshir
            if (Bukkit.getPluginManager().getPlugin("floodgate") == null) {
                this.floodgateLoaded = false;
                return;
            }
            // Floodgate API class ham mavjudligini tekshir
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            this.bridge = new FloodgateBridge(plugin);
            this.floodgateLoaded = true;
        } catch (Throwable t) {
            this.floodgateLoaded = false;
            this.bridge = null;
            plugin.getLogger().info("Floodgate Bridge initiate qilinmadi: " + t.getMessage());
        }
    }

    public boolean isFloodgateLoaded() {
        return floodgateLoaded;
    }

    public boolean isBedrockPlayer(UUID id) {
        if (!floodgateLoaded || bridge == null) return false;
        try {
            return bridge.isFloodgatePlayer(id);
        } catch (Throwable t) {
            return false;
        }
    }

    public void sendForm(Player player, CaptchaSession session) {
        if (!floodgateLoaded || bridge == null) return;
        try {
            bridge.sendCaptchaForm(player, session);
        } catch (Throwable t) {
            plugin.getLogger().warning("Bedrock form yuborilmadi: " + t.getMessage());
        }
    }
}
