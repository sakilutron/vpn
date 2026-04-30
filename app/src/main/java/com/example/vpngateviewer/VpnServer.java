package com.example.vpngateviewer;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable data model representing a VPN server entry from the VPN Gate API.
 * <p>
 * Mutable transient state (favorite, newlyAdded) is managed externally and
 * provided via separate setters to keep construction clean. Use {@link #getCacheKey()}
 * for identity-based comparisons.
 */
public class VpnServer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String hostName;
    private final String ip;
    private final int score;
    private final int ping;
    private final long speed;
    private final String countryLong;
    private final String countryShort;
    private final long numVpnSessions;
    private final long uptime;
    private final long totalUsers;
    private final long totalTraffic;
    private final String logType;
    private final String operator;
    private final String message;
    private final String openVPNConfigDataBase64;

    // ---- transient mutable flags ----
    private boolean favorite;
    private boolean newlyAdded;

    public VpnServer(String hostName, String ip, int score, int ping, long speed,
                     String countryLong, String countryShort, long numVpnSessions,
                     long uptime, long totalUsers, long totalTraffic, String logType,
                     String operator, String message, String openVPNConfigDataBase64) {
        this.hostName = hostName;
        this.ip = ip;
        this.score = score;
        this.ping = ping;
        this.speed = speed;
        this.countryLong = countryLong;
        this.countryShort = countryShort;
        this.numVpnSessions = numVpnSessions;
        this.uptime = uptime;
        this.totalUsers = totalUsers;
        this.totalTraffic = totalTraffic;
        this.logType = logType;
        this.operator = operator;
        this.message = message;
        this.openVPNConfigDataBase64 = openVPNConfigDataBase64;
    }

    // ---- transient mutators ----

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setNewlyAdded(boolean newlyAdded) {
        this.newlyAdded = newlyAdded;
    }

    public boolean isNewlyAdded() {
        return newlyAdded;
    }

    // ---- identity ----

    /**
     * Returns a stable identity key for caching and favorites.
     * Falls back to hostname when IP is null (should not happen in practice).
     */
    public String getCacheKey() {
        return ip != null ? ip : hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VpnServer)) return false;
        VpnServer that = (VpnServer) o;
        return Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ip);
    }

    // ---- accessors ----

    public String getHostName() {
        return hostName;
    }

    public String getIp() {
        return ip;
    }

    public int getScore() {
        return score;
    }

    public int getPing() {
        return ping;
    }

    /** Speed in bits per second. */
    public long getSpeed() {
        return speed;
    }

    public String getCountryLong() {
        return countryLong;
    }

    public String getCountryShort() {
        return countryShort;
    }

    public long getNumVpnSessions() {
        return numVpnSessions;
    }

    public long getUptime() {
        return uptime;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalTraffic() {
        return totalTraffic;
    }

    public String getLogType() {
        return logType;
    }

    public String getOperator() {
        return operator;
    }

    public String getMessage() {
        return message;
    }

    public String getOpenVPNConfigDataBase64() {
        return openVPNConfigDataBase64;
    }

    @Override
    public String toString() {
        return "VpnServer{"
                + "hostName='" + hostName + '\''
                + ", ip='" + ip + '\''
                + ", countryLong='" + countryLong + '\''
                + '}';
    }
}