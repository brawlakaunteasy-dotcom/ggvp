package com.exantibotfilter.captcha;

import com.exantibotfilter.ExAntiBotFilter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hammasi shu yerdan boshqariladi: sessiya yaratish, menyu ochish, tekshirish, kick.
 */
public class CaptchaManager {

    private final ExAntiBotFilter plugin;
    private final Map<UUID, CaptchaSession> sessions = new HashMap<>();

    public CaptchaManager(ExAntiBotFilter plugin) {
        this.plugin = plugin;
    }

    public boolean isInCaptcha(UUID id) {
        CaptchaSession s = sessions.get(id);
        return s != null && !s.isFinished();
    }

    public CaptchaSession getSession(UUID id) {
        return sessions.get(id);
    }

    /**
     * Captchani boshlash. Player ulanish vaqtida yoki keyin chaqiriladi.
     */
    public void start(Player player) {
        if (player.hasPermission("exantibotfilter.bypass")) {
            return;
        }
        UUID id = player.getUniqueId();
        // Eski sessiyani tozalash
        CaptchaSession old = sessions.remove(id);
        if (old != null && old.getTimeoutTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(old.getTimeoutTaskId());
        }

        CaptchaSession session = new CaptchaSession(id, player.getLocation());
        session.setPage(1);
        sessions.put(id, session);

        // Bedrock playerga form yuborish
        boolean useBedrock = plugin.getBedrockSupport().isBedrockPlayer(id)
                && plugin.getConfig().getBoolean("bedrock.use-form", true);

        if (useBedrock) {
            plugin.getBedrockSupport().sendForm(player, session);
        } else {
            openMenu(player, session);
        }

        startTimeout(player, session);
    }

    /**
     * Java menyu ochish (1 tick keyin - join paytida darhol ochib bo'lmaydi).
     */
    public void openMenu(final Player player, final CaptchaSession session) {
        session.resetForNewPage();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || session.isFinished()) return;
                Inventory inv = CaptchaMenu.build(plugin, session);
                session.setPendingReopen(false);
                player.openInventory(inv);
            }
        }.runTaskLater(plugin, 2L);
    }

    private void startTimeout(final Player player, final CaptchaSession session) {
        if (session.getTimeoutTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getTimeoutTaskId());
        }
        int seconds = plugin.getConfig().getInt("captcha.timeout-seconds", 60);
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (session.isFinished()) return;
                if (!player.isOnline()) return;
                kickTimeout(player);
            }
        }.runTaskLater(plugin, seconds * 20L).getTaskId();
        session.setTimeoutTaskId(taskId);
    }

    /**
     * Slotni bosgandagi tekshiruv.
     * action: LEFT/RIGHT click bo'lishi mumkin. Biz right-click ni qabul qilamiz.
     */
    public void handleSlotClick(Player player, int slot, boolean rightClick) {
        UUID id = player.getUniqueId();
        CaptchaSession session = sessions.get(id);
        if (session == null || session.isFinished()) return;

        boolean isCookie = session.getCookieSlots().contains(slot);

        if (isCookie) {
            // Faqat right-click qabul qilamiz (foydalanuvchi shartnomasi)
            if (!rightClick) {
                // Left click cookie'da - hech narsa qilmaymiz, jim turamiz
                return;
            }
            session.getClickedCookies().add(slot);
            if (session.isAllCookiesClicked()) {
                pass(player, session);
            }
        } else {
            // Noto'g'ri item - sahifani 2 ga o'zgartirib yoki kick
            session.incrementWrongPages();
            int max = plugin.getConfig().getInt("captcha.max-wrong-pages", 2);
            if (session.getWrongPages() >= max) {
                kickWrong(player, session);
                return;
            }
            // Yangi sahifa
            session.setPage(2);
            session.setPendingReopen(true);
            // Avval inventarni yopamiz, keyin yangi menyu
            player.closeInventory();
            openMenu(player, session);
        }
    }

    /**
     * Tasdiqlandi.
     */
    public void pass(Player player, CaptchaSession session) {
        session.setFinished(true);
        if (session.getTimeoutTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getTimeoutTaskId());
            session.setTimeoutTaskId(-1);
        }
        sessions.remove(session.getPlayerId());

        // Inventarni yopish
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.closeInventory();
                    String msg = plugin.color(plugin.getConfig().getString("messages.passed", "&aSiz bot emasligingiz tasdiqlandi!"));
                    player.sendMessage(plugin.prefix() + msg);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    public void kickWrong(final Player player, CaptchaSession session) {
        finishAndKick(player, session, plugin.getConfig().getString("messages.kick-bot", "&cSiz bot deb topildingiz."));
    }

    public void kickTimeout(Player player) {
        CaptchaSession session = sessions.get(player.getUniqueId());
        finishAndKick(player, session, plugin.getConfig().getString("messages.kick-timeout", "&cVaqt tugadi! Qaytadan urining."));
    }

    private void finishAndKick(final Player player, CaptchaSession session, final String reason) {
        if (session != null) {
            session.setFinished(true);
            if (session.getTimeoutTaskId() != -1) {
                Bukkit.getScheduler().cancelTask(session.getTimeoutTaskId());
                session.setTimeoutTaskId(-1);
            }
            sessions.remove(session.getPlayerId());
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.kickPlayer(plugin.color(reason));
                }
            }
        }.runTask(plugin);
    }

    /**
     * Player chiqib ketdi - sessiyani tozalash.
     */
    public void cleanup(UUID id) {
        CaptchaSession s = sessions.remove(id);
        if (s != null) {
            s.setFinished(true);
            if (s.getTimeoutTaskId() != -1) {
                Bukkit.getScheduler().cancelTask(s.getTimeoutTaskId());
            }
        }
    }

    public void shutdown() {
        for (CaptchaSession s : sessions.values()) {
            if (s.getTimeoutTaskId() != -1) {
                Bukkit.getScheduler().cancelTask(s.getTimeoutTaskId());
            }
            s.setFinished(true);
        }
        sessions.clear();
    }
}
