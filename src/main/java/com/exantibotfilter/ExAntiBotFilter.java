package com.exantibotfilter;

import com.exantibotfilter.bedrock.BedrockSupport;
import com.exantibotfilter.captcha.CaptchaManager;
import com.exantibotfilter.command.AdminCommand;
import com.exantibotfilter.listener.ProtectionListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExAntiBotFilter
 * Java 8 - 25 mos. Spigot/Paper 1.13+ serverlar uchun.
 * Geyser + Floodgate orqali Bedrock playerlarni qo'llab-quvvatlaydi.
 */
public class ExAntiBotFilter extends JavaPlugin {

    private static ExAntiBotFilter instance;

    private CaptchaManager captchaManager;
    private BedrockSupport bedrockSupport;

    @Override
    public void onEnable() {
        instance = this;

        // Konfiguratsiyani yuklash
        saveDefaultConfig();
        reloadConfig();

        // Bedrock support (Floodgate optional)
        this.bedrockSupport = new BedrockSupport(this);
        this.bedrockSupport.init();

        // Captcha manager
        this.captchaManager = new CaptchaManager(this);

        // Listenerlarni ro'yxatga olish
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);

        // Buyruqni ro'yxatga olish
        if (getCommand("exantibotfilter") != null) {
            getCommand("exantibotfilter").setExecutor(new AdminCommand(this));
        }

        getLogger().info("ExAntiBotFilter yoqildi. Java versiyasi: " + System.getProperty("java.version"));
        if (bedrockSupport.isFloodgateLoaded()) {
            getLogger().info("Floodgate aniqlandi - Bedrock playerlarga Form menyusi ishlatiladi.");
        } else {
            getLogger().info("Floodgate topilmadi - faqat Java menyusi ishlatiladi.");
        }
    }

    @Override
    public void onDisable() {
        if (captchaManager != null) {
            captchaManager.shutdown();
        }
        instance = null;
    }

    public static ExAntiBotFilter get() {
        return instance;
    }

    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    public BedrockSupport getBedrockSupport() {
        return bedrockSupport;
    }

    public String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public String prefix() {
        return color(getConfig().getString("messages.prefix", "&8[&6AntiBot&8] "));
    }
}
