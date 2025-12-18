package com.example.vpngateviewer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VpnGateClient {

    private static final String API_URL = "https://www.vpngate.net/api/iphone/";

    public List<VpnServer> fetchVpnServers() throws Exception {
        List<VpnServer> vpnServers = new ArrayList<>();
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            boolean headerFound = false;
            
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.startsWith("*") || line.trim().isEmpty()) {
                    continue;
                }
                
                if (line.startsWith("#")) {
                    headerFound = true;
                    continue; // Skip header line
                }
                
                if (!headerFound) continue;

                String[] parts = line.split(",");
                // Basic validation: ensure we have enough columns (15 columns based on the header)
                if (parts.length >= 15) {
                   try {
                       String hostName = parts[0];
                       String ip = parts[1];
                       int score = Integer.parseInt(parts[2]);
                       int ping = Integer.parseInt(parts[3]);
                       long speed = Long.parseLong(parts[4]);
                       String countryLong = parts[5];
                       String countryShort = parts[6];
                       long numVpnSessions = Long.parseLong(parts[7]);
                       long uptime = Long.parseLong(parts[8]);
                       long totalUsers = Long.parseLong(parts[9]);
                       long totalTraffic = Long.parseLong(parts[10]);
                       String logType = parts[11];
                       String operator = parts[12];
                       String message = parts[13];
                       String openVPNConfigDataBase64 = parts[14];

                       VpnServer server = new VpnServer(
                               hostName, ip, score, ping, speed, countryLong, countryShort,
                               numVpnSessions, uptime, totalUsers, totalTraffic, logType,
                               operator, message, openVPNConfigDataBase64
                       );
                       vpnServers.add(server);
                   } catch (NumberFormatException e) {
                       // Skip malformed lines
                       System.err.println("Skipping malformed line: " + line);
                   }
                }
            }
        }
        
        return vpnServers;
    }
}
