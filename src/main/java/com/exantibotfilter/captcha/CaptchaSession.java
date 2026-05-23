package com.exantibotfilter.captcha;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Bitta playerning captcha sessiyasi.
 */
public class CaptchaSession {

    private final UUID playerId;
    private final Location frozenLocation;

    /** 1 yoki 2 (sahifa) */
    private int page = 1;

    /** Joriy menyudagi cookie slotlari */
    private final Set<Integer> cookieSlots = new HashSet<>();

    /** Bosilgan cookie slotlari */
    private final Set<Integer> clickedCookies = new HashSet<>();

    /** Necha marta noto'g'ri tanladi (page-larni o'tdi) */
    private int wrongPages = 0;

    /** Menyu yopilishi qayta ochishni anglatadi (true bo'lsa close eventni e'tibor bermay reopen qiladi) */
    private boolean pendingReopen = false;

    /** Timeout task ID */
    private int timeoutTaskId = -1;

    /** Tugaganmi (passed yoki kicked) */
    private boolean finished = false;

    public CaptchaSession(UUID playerId, Location frozenLocation) {
        this.playerId = playerId;
        this.frozenLocation = frozenLocation == null ? null : frozenLocation.clone();
    }

    public UUID getPlayerId() { return playerId; }
    public Location getFrozenLocation() { return frozenLocation == null ? null : frozenLocation.clone(); }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public Set<Integer> getCookieSlots() { return cookieSlots; }
    public Set<Integer> getClickedCookies() { return clickedCookies; }

    public int getWrongPages() { return wrongPages; }
    public void incrementWrongPages() { this.wrongPages++; }

    public boolean isPendingReopen() { return pendingReopen; }
    public void setPendingReopen(boolean pendingReopen) { this.pendingReopen = pendingReopen; }

    public int getTimeoutTaskId() { return timeoutTaskId; }
    public void setTimeoutTaskId(int timeoutTaskId) { this.timeoutTaskId = timeoutTaskId; }

    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }

    public void resetForNewPage() {
        cookieSlots.clear();
        clickedCookies.clear();
        pendingReopen = false;
    }

    public boolean isAllCookiesClicked() {
        return !cookieSlots.isEmpty() && clickedCookies.containsAll(cookieSlots);
    }
}
