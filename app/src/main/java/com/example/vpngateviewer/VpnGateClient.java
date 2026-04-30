package com.example.vpngateviewer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class VpnGateClient {

    private static final String TAG = "VpnGateClient";
    private static final String API_URL = "https://www.vpngate.net/api/iphone/";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int HTTP_OK = 200;
    private static final int MIN_COLUMNS = 15;
    private static final int PREALLOCATED_CAPACITY = 200;

    /**
     * Fetches VPN server list from the VPN Gate API.
     * <p>
     * Uses connect/read timeouts to prevent hanging and supports gzip
     * compressed responses for reduced bandwidth.
     *
     * @return unmodifiable list of parsed VPN servers
     * @throws IOException on network or parsing failure
     */
    public List<VpnServer> fetchVpnServers() throws IOException {
        List<VpnServer> vpnServers = new ArrayList<>(PREALLOCATED_CAPACITY);
        URL url = new URL(API_URL);
        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("User-Agent", "VPNGateViewer/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != HTTP_OK) {
                throw new IOException("HTTP " + responseCode + " from VPN Gate API");
            }

            InputStream inputStream = conn.getInputStream();
            String contentEncoding = conn.getContentEncoding();
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                inputStream = new GZIPInputStream(inputStream);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                parseCsvResponse(reader, vpnServers);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        Log.i(TAG, "Fetched " + vpnServers.size() + " VPN servers");
        return Collections.unmodifiableList(vpnServers);
    }

    private void parseCsvResponse(BufferedReader reader, List<VpnServer> out)
            throws IOException {
        String line;
        boolean headerFound = false;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("*") || line.trim().isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                headerFound = true;
                continue;
            }

            if (!headerFound) {
                continue;
            }

            VpnServer server = parseLine(line);
            if (server != null) {
                out.add(server);
            }
        }
    }

    private VpnServer parseLine(String line) {
        String[] parts = line.split(",");
        if (parts.length < MIN_COLUMNS) {
            return null;
        }

        try {
            return new VpnServer(
                    parts[0],                                    // hostName
                    parts[1],                                    // ip
                    parseIntSafe(parts[2]),                      // score
                    parseIntSafe(parts[3]),                      // ping
                    parseLongSafe(parts[4]),                     // speed
                    parts[5],                                    // countryLong
                    parts[6],                                    // countryShort
                    parseLongSafe(parts[7]),                     // numVpnSessions
                    parseLongSafe(parts[8]),                     // uptime
                    parseLongSafe(parts[9]),                     // totalUsers
                    parseLongSafe(parts[10]),                    // totalTraffic
                    parts[11],                                   // logType
                    parts[12],                                   // operator
                    parts[13],                                   // message
                    parts[14]                                    // openVPNConfigDataBase64
            );
        } catch (Exception e) {
            Log.w(TAG, "Skipping malformed line", e);
            return null;
        }
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLongSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
