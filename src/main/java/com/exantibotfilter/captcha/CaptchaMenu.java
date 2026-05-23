package com.exantibotfilter.captcha;

import com.exantibotfilter.ExAntiBotFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 54 slotli cookie tanlash menyusi.
 */
public class CaptchaMenu {

    private static final int SIZE = 54;
    private static final Random RANDOM = new Random();

    public static final String HOLDER_TAG = "ExAntiBotFilter-Captcha";

    /** Bizning menyu ekanligini bilish uchun maxsus holder */
    public static class Holder implements InventoryHolder {
        private final UUID playerId;
        private final int page;
        private Inventory inventory;

        public Holder(UUID playerId, int page) {
            this.playerId = playerId;
            this.page = page;
        }

        public UUID getPlayerId() { return playerId; }
        public int getPage() { return page; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }

        @Override
        public Inventory getInventory() { return inventory; }
    }

    /**
     * Menyu yaratadi va sessiyaga cookie slotlarini yozib qo'yadi.
     */
    public static Inventory build(ExAntiBotFilter plugin, CaptchaSession session) {
        int page = session.getPage();
        String titleKey = page == 1 ? "captcha.title-page1" : "captcha.title-page2";
        String defTitle = page == 1 ? "&8Cookie tanlang &7(1/2)" : "&cQayta urining &7(2/2)";
        String title = plugin.color(plugin.getConfig().getString(titleKey, defTitle));

        Holder holder = new Holder(session.getPlayerId(), page);
        // Title 32 belgidan oshmasligi kerak (1.8 limit). 1.13+ da limit yo'q.
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        // Cookie slotlari
        int cookieCount = page == 1
                ? plugin.getConfig().getInt("captcha.cookies-page1", 5)
                : plugin.getConfig().getInt("captcha.cookies-page2", 5);
        if (cookieCount < 1) cookieCount = 1;
        if (cookieCount > SIZE) cookieCount = SIZE;

        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) allSlots.add(i);
        Collections.shuffle(allSlots, RANDOM);

        Set<Integer> cookieSlots = session.getCookieSlots();
        cookieSlots.clear();
        for (int i = 0; i < cookieCount; i++) {
            cookieSlots.add(allSlots.get(i));
        }

        // Cookie itemini yaratish
        ItemStack cookieItem = createCookie(plugin);

        // Filler materiallar
        List<Material> fillers = loadFillerMaterials(plugin);
        if (fillers.isEmpty()) {
            fillers = Arrays.asList(Material.APPLE, Material.BREAD);
        }

        for (int slot = 0; slot < SIZE; slot++) {
            if (cookieSlots.contains(slot)) {
                inv.setItem(slot, cookieItem.clone());
            } else {
                Material m = fillers.get(RANDOM.nextInt(fillers.size()));
                ItemStack item = new ItemStack(m);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.color("&e" + prettyName(m.name())));
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }

        return inv;
    }

    private static ItemStack createCookie(ExAntiBotFilter plugin) {
        Material cookieMat = matchMaterial("COOKIE");
        if (cookieMat == null) cookieMat = Material.valueOf("COOKIE");
        ItemStack item = new ItemStack(cookieMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.color(plugin.getConfig().getString("captcha.cookie-name", "&6&lCookie &7(bosing)"));
            meta.setDisplayName(name);
            List<String> lore = plugin.getConfig().getStringList("captcha.cookie-lore");
            List<String> colored = new ArrayList<>();
            for (String s : lore) colored.add(plugin.color(s));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<Material> loadFillerMaterials(ExAntiBotFilter plugin) {
        List<String> names = plugin.getConfig().getStringList("captcha.filler-materials");
        List<Material> result = new ArrayList<>();
        for (String n : names) {
            Material m = matchMaterial(n);
            if (m != null && m != matchMaterial("COOKIE") && m.isItem()) {
                result.add(m);
            }
        }
        return result;
    }

    private static Material matchMaterial(String name) {
        if (name == null) return null;
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // 1.13+ da matchMaterial mavjud, lekin biz Material.valueOf bilan ishlayapmiz.
            return null;
        }
    }

    private static String prettyName(String s) {
        return s.toLowerCase().replace('_', ' ');
    }

    /**
     * Berilgan inventory bizning captcha menyusi ekanligini tekshiradi.
     */
    public static Holder getHolder(Inventory inv) {
        if (inv == null) return null;
        if (inv.getType() != InventoryType.CHEST) return null;
        InventoryHolder h = inv.getHolder();
        if (h instanceof Holder) return (Holder) h;
        return null;
    }
}
