package com.example.vpngateviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements VpnServerListFragment.OnVpnServerClickListener {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView errorText;
    private CountryPagerAdapter countryPagerAdapter;
    private Set<String> cachedServerKeys;
    private Set<String> favoriteServerKeys;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VpnGateClient vpnGateClient = new VpnGateClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);

        cachedServerKeys = new HashSet<>(getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                .getStringSet("cached_servers", new HashSet<>()));
        favoriteServerKeys = new HashSet<>(getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                .getStringSet("favorite_servers", new HashSet<>()));

        fetchVpnData();
    }

    private void fetchVpnData() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);

        executorService.execute(() -> {
            try {
                List<VpnServer> servers = vpnGateClient.fetchVpnServers();
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (servers.isEmpty()) {
                        errorText.setText("No VPN servers found.");
                        errorText.setVisibility(View.VISIBLE);
                    } else {
                        setupCountryTabs(servers);
                        viewPager.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    errorText.setText("Error loading data: " + e.getMessage());
                    errorText.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void setupCountryTabs(List<VpnServer> servers) {
        Map<String, List<VpnServer>> groupedByCountry = new LinkedHashMap<>();
        Set<String> nextCachedKeys = new HashSet<>();
        for (VpnServer server : servers) {
            String countryCode = server.getCountryShort();
            String cacheKey = server.getCacheKey();
            boolean isNew = !cachedServerKeys.contains(cacheKey);
            server.setNewlyAdded(isNew);
            boolean isFavorite = favoriteServerKeys.contains(cacheKey);
            server.setFavorite(isFavorite);
            nextCachedKeys.add(cacheKey);
            groupedByCountry.computeIfAbsent(countryCode, key -> new ArrayList<>()).add(server);
        }

        List<CountryTab> countryTabs = new ArrayList<>();
        for (Map.Entry<String, List<VpnServer>> entry : groupedByCountry.entrySet()) {
            List<VpnServer> sortedServers = new ArrayList<>(entry.getValue());
            Collections.sort(sortedServers, Comparator
                    .comparing(VpnServer::isNewlyAdded, Comparator.reverseOrder())
                    .thenComparing(VpnServer::isFavorite, Comparator.reverseOrder())
                    .thenComparingInt(VpnServer::getPing));
            String countryCode = entry.getKey();
            String countryName = sortedServers.isEmpty() ? countryCode : sortedServers.get(0).getCountryLong();
            countryTabs.add(new CountryTab(countryName, countryCode, sortedServers));
        }

        Collections.sort(countryTabs, (left, right) -> {
            if (left.getCountryCode().equalsIgnoreCase("US")) return -1;
            if (right.getCountryCode().equalsIgnoreCase("US")) return 1;
            return left.getCountryName().compareTo(right.getCountryName());
        });

        countryPagerAdapter = new CountryPagerAdapter(this, countryTabs);
        viewPager.setAdapter(countryPagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(countryPagerAdapter.getPageTitle(position))).attach();

        cachedServerKeys = nextCachedKeys;
        getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                .edit()
                .putStringSet("cached_servers", new HashSet<>(cachedServerKeys))
                .apply();
    }

    private void onConnectClick(VpnServer server) {
        String base64Config = server.getOpenVPNConfigDataBase64();
        if (base64Config == null || base64Config.isEmpty()) {
            Toast.makeText(this, "No OpenVPN config available for this server.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] configData = Base64.decode(base64Config, Base64.DEFAULT);
            File cachePath = new File(getCacheDir(), "vpn_profiles");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            String filename = "vpngate_" + server.getIp().replace(".", "_") + ".ovpn";
            File newFile = new File(cachePath, filename);

            try (FileOutputStream fos = new FileOutputStream(newFile)) {
                fos.write(configData);
            }

            Uri contentUri = FileProvider.getUriForFile(this, "com.example.vpngateviewer.fileprovider", newFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/x-openvpn-profile");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing VPN config: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not open VPN profile. Do you have an OpenVPN app installed?", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemClick(VpnServer server) {
        onConnectClick(server);
    }

    @Override
    public void onFavoriteToggle(VpnServer server) {
        String key = server.getCacheKey();
        if (server.isFavorite()) {
            favoriteServerKeys.add(key);
        } else {
            favoriteServerKeys.remove(key);
        }
        getSharedPreferences("vpn_prefs", MODE_PRIVATE)
                .edit()
                .putStringSet("favorite_servers", new HashSet<>(favoriteServerKeys))
                .apply();
        Toast.makeText(this, server.isFavorite() ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
    }
}
