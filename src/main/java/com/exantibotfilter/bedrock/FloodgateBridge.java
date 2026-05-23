package com.exantibotfilter.bedrock;

import com.exantibotfilter.ExAntiBotFilter;
import com.exantibotfilter.captcha.CaptchaManager;
import com.exantibotfilter.captcha.CaptchaSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Floodgate API ni to'g'ridan-to'g'ri ishlatadigan bridge.
 * Bu class faqat Floodgate yuklangan bo'lsa load qilinadi (BedrockSupport.init() orqali).
 *
 * Bedrock UI cheklovlari sababli, Java versiyasidagi 5 ta cookie ni ketma-ket bossa
 * tanlashga asoslangan bo'lib, har gal bittadan tugma bosib boriladi.
 */
public class FloodgateBridge {

    private final ExAntiBotFilter plugin;
    private final Random random = new Random();

    public FloodgateBridge(ExAntiBotFilter plugin) {
        this.plugin = plugin;
    }

    public boolean isFloodgatePlayer(UUID id) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(id);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Bedrock playerga captcha formasini yuborish. Har bir cookie bosilganda
     * sessiya yangilanadi va yangi forma yuboriladi (cookies-page1 marta).
     */
    public void sendCaptchaForm(final Player player, final CaptchaSession session) {
        if (session.isFinished()) return;
        final UUID id = player.getUniqueId();

        int requiredCookies = session.getPage() == 1
                ? plugin.getConfig().getInt("captcha.cookies-page1", 5)
                : plugin.getConfig().getInt("captcha.cookies-page2", 5);

        int progress = session.getClickedCookies().size();
        int remaining = requiredCookies - progress;

        // Sarlavha va kontent
        String titleKey = session.getPage() == 1 ? "bedrock.form-content-page1" : "bedrock.form-content-page2";
        String title = plugin.getConfig().getString("bedrock.form-title", "Bot tekshiruvi");
        String content = plugin.getConfig().getString(titleKey,
                session.getPage() == 1
                        ? "Cookie tugmalarini bosing.\nSahifa: 1/2"
                        : "Qayta urining! Cookie tugmalarini bosing.\nSahifa: 2/2");
        content += "\n\nQolgan cookie: " + remaining + "/" + requiredCookies;

        // Tugmalar - 1 ta cookie + bir nechta fake (ketma-ket bosish)
        final List<String> cookieLabels = new ArrayList<String>();
        cookieLabels.add(plugin.getConfig().getString("bedrock.form-cookie-button", "Cookie"));

        List<String> fakes = plugin.getConfig().getStringList("bedrock.form-fake-buttons");
        if (fakes == null || fakes.isEmpty()) {
            fakes = new ArrayList<String>();
            Collections.addAll(fakes, "Olma", "Non", "Tort", "Sabzi", "Kartoshka");
        }

        // Buttonlar tartibini shuffle qilamiz: 1 cookie + 5 fake
        List<String> buttons = new ArrayList<String>();
        buttons.add(cookieLabels.get(0)); // 0-index = cookie (keyin shuffle bo'ladi)
        int fakeCount = Math.min(5, fakes.size());
        Collections.shuffle(fakes, random);
        for (int i = 0; i < fakeCount; i++) {
            buttons.add(fakes.get(i));
        }

        // Cookie qaysi indekste ekanligini saqlash uchun shuffle qilamiz
        // va indeksini topib qo'yamiz
        Collections.shuffle(buttons, random);
        final int cookieIndex = buttons.indexOf(plugin.getConfig().getString("bedrock.form-cookie-button", "Cookie"));

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(title)
                .content(content);
        for (String b : buttons) {
            builder.button(b);
        }
        builder.validResultHandler(new java.util.function.Consumer<SimpleFormResponse>() {
            @Override
            public void accept(SimpleFormResponse resp) {
                handleResponse(player, session, resp.clickedButtonId(), cookieIndex);
            }
        });
        // Closed yoki invalid - menyu yopildi - qayta ochamiz
        builder.closedOrInvalidResultHandler(new Runnable() {
            @Override
            public void run() {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline() || session.isFinished()) return;
                        if (plugin.getCaptchaManager().isInCaptcha(id)) {
                            sendCaptchaForm(player, session);
                        }
                    }
                });
            }
        });

        SimpleForm form = builder.build();
        FloodgateApi.getInstance().sendForm(id, form);
    }

    private void handleResponse(final Player player, final CaptchaSession session, int clickedIndex, int cookieIndex) {
        if (session.isFinished()) return;
        final CaptchaManager mgr = plugin.getCaptchaManager();

        if (clickedIndex == cookieIndex) {
            // To'g'ri - cookie bosildi
            session.getClickedCookies().add(clickedIndex);

            int requiredCookies = session.getPage() == 1
                    ? plugin.getConfig().getInt("captcha.cookies-page1", 5)
                    : plugin.getConfig().getInt("captcha.cookies-page2", 5);

            if (session.getClickedCookies().size() >= requiredCookies) {
                // Tasdiqlandi
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        mgr.pass(player, session);
                    }
                });
            } else {
                // Yana forma yuborish
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        sendCaptchaForm(player, session);
                    }
                });
            }
        } else {
            // Noto'g'ri tanlov
            session.incrementWrongPages();
            int max = plugin.getConfig().getInt("captcha.max-wrong-pages", 2);
            if (session.getWrongPages() >= max) {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        mgr.kickWrong(player, session);
                    }
                });
            } else {
                // 2-sahifaga o'tish
                session.setPage(2);
                session.resetForNewPage();
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        sendCaptchaForm(player, session);
                    }
                });
            }
        }
    }
}
