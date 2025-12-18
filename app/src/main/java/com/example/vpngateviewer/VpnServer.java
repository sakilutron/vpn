package com.example.vpngateviewer;

import java.io.Serializable;

public class VpnServer implements Serializable {
    private String hostName;
    private String ip;
    private int score;
    private int ping;
    private long speed;
    private String countryLong;
    private String countryShort;
    private long numVpnSessions;
    private long uptime;
    private long totalUsers;
    private long totalTraffic;
    private String logType;
    private String operator;
    private String message;
    private String openVPNConfigDataBase64;
    private boolean favorite;
    private boolean newlyAdded;

    public VpnServer(String hostName, String ip, int score, int ping, long speed, String countryLong, String countryShort, long numVpnSessions, long uptime, long totalUsers, long totalTraffic, String logType, String operator, String message, String openVPNConfigDataBase64) {
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

    public String getCacheKey() {
        return ip != null ? ip : hostName;
    }

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

    public long getSpeed() {
        return speed;
    }

    public String getCountryLong() {
        return countryLong;
    }

    public String getCountryShort() {
        return countryShort;
    }

    public String getOpenVPNConfigDataBase64() {
        return openVPNConfigDataBase64;
    }

    @Override
    public String toString() {
        return "VpnServer{" +
                "hostName='" + hostName + '\'' +
                ", ip='" + ip + '\'' +
                ", countryLong='" + countryLong + '\'' +
                '}';
    }
}
