package com.example.vpngateviewer;

import java.util.List;

public class CountryTab {
    private final String countryName;
    private final String countryCode;
    private final List<VpnServer> servers;
    private final String overrideTitle;

    public CountryTab(String countryName, String countryCode, List<VpnServer> servers) {
        this(countryName, countryCode, servers, null);
    }

    public CountryTab(String countryName, String countryCode, List<VpnServer> servers, String overrideTitle) {
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.servers = servers;
        this.overrideTitle = overrideTitle;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public List<VpnServer> getServers() {
        return servers;
    }

    public String getTitleWithFlag() {
        if (overrideTitle != null) {
            return overrideTitle;
        }
        String flag = CountryFlagUtils.countryCodeToFlag(countryCode);
        return (flag.isEmpty() ? "" : flag + " ") + countryName;
    }
}
