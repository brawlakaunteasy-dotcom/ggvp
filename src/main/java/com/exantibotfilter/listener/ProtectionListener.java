package com.exantibotfilter.listener;

import com.exantibotfilter.ExAntiBotFilter;
import com.exantibotfilter.captcha.CaptchaManager;
import com.exantibotfilter.captcha.CaptchaMenu;
import com.exantibotfilter.captcha.CaptchaSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ProtectionListener implements Listener {

    private final ExAntiBotFilter plugin;

    public ProtectionListener(ExAntiBotFilter plugin) {
        this.plugin = plugin;
    }

    private CaptchaManager mgr() { return plugin.getCaptchaManager(); }

    // === JOIN ===
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(final PlayerJoinEvent event) {
        // Boshqa pluginlar yuborgan join xabarini olib tashlamaymiz, lekin captcha boshlaymiz
        final Player p = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline() && !p.hasPermission("exantibotfilter.bypass")) {
                    mgr().start(p);
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        mgr().cleanup(event.getPlayer().getUniqueId());
    }

    // === MENU CLICK ===
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();

        CaptchaMenu.Holder holder = CaptchaMenu.getHolder(event.getInventory());

        // Captchada bo'lsa hech qachon item olib qo'ya olmaydi
        if (mgr().isInCaptcha(p.getUniqueId())) {
            event.setCancelled(true);
            // Faqat o'zining captcha menyusida bossa - tekshir
            if (holder != null && holder.getPlayerId().equals(p.getUniqueId())) {
                if (event.getRawSlot() < 0 || event.getRawSlot() >= 54) return; // o'z inventari
                boolean right = event.getClick() != null && event.getClick().isRightClick();
                mgr().handleSlotClick(p, event.getRawSlot(), right);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        if (mgr().isInCaptcha(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        final Player p = (Player) event.getPlayer();
        UUID id = p.getUniqueId();
        if (!mgr().isInCaptcha(id)) return;

        CaptchaMenu.Holder holder = CaptchaMenu.getHolder(event.getInventory());
        if (holder == null) return;

        final CaptchaSession session = mgr().getSession(id);
        if (session == null || session.isFinished()) return;

        // Player menyuni yopdi - qayta ochamiz (Bedrock playerlar uchun ham bo'lmasin chunki ularda Form)
        if (plugin.getBedrockSupport().isBedrockPlayer(id)
                && plugin.getConfig().getBoolean("bedrock.use-form", true)) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline() && mgr().isInCaptcha(p.getUniqueId())) {
                    mgr().openMenu(p, session);
                }
            }
        }.runTaskLater(plugin, 3L);
    }

    // === CHAT ===
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        if (mgr().isInCaptcha(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            // Silent - umuman hech narsa yozmaydi
        }
    }

    // === COMMANDS ===
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (!mgr().isInCaptcha(p.getUniqueId())) return;
        String msg = event.getMessage();
        if (msg == null || !msg.startsWith("/")) return;
        String cmd = msg.substring(1).split(" ", 2)[0].toLowerCase();
        // Plugin nomi yoki namespace bo'lsa olib tashlash (masalan: /minecraft:server)
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }
        for (String allowed : plugin.getConfig().getStringList("protection.allowed-commands")) {
            if (allowed.equalsIgnoreCase(cmd)) {
                return; // ruxsat
            }
        }
        event.setCancelled(true);
    }

    // === MOVE (joyni yo'qotmasligi uchun) ===
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("protection.block-movement", true)) return;
        Player p = event.getPlayer();
        if (!mgr().isInCaptcha(p.getUniqueId())) return;
        if (event.getTo() == null || event.getFrom() == null) return;
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            CaptchaSession s = mgr().getSession(p.getUniqueId());
            Location target = (s != null && s.getFrozenLocation() != null) ? s.getFrozenLocation() : event.getFrom();
            // Yawni saqlaymiz - qarashga ruxsat
            target.setYaw(event.getTo().getYaw());
            target.setPitch(event.getTo().getPitch());
            event.setTo(target);
        }
    }

    // === DAMAGE ===
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("protection.block-damage", true)) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (mgr().isInCaptcha(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player p = (Player) event.getDamager();
            if (mgr().isInCaptcha(p.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // === INTERACT ===
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("protection.block-interact", true)) return;
        Player p = event.getPlayer();
        if (mgr().isInCaptcha(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (mgr().isInCaptcha(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (mgr().isInCaptcha(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (mgr().isInCaptcha(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfig().getBoolean("protection.block-item-pickup", true)) return;
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (mgr().isInCaptcha(p.getUniqueId())) event.setCancelled(true);
        }
    }
}
