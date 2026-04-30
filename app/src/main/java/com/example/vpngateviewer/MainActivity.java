package com.example.vpngateviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity
        implements VpnServerListFragment.OnVpnServerClickListener {

    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "vpn_prefs";
    private static final String KEY_CACHED_SERVERS = "cached_servers";
    private static final String KEY_FAVORITE_SERVERS = "favorite_servers";
    private static final long REFRESH_INTERVAL_MS = 30_000L;
    private static final String FILE_PROVIDER_AUTHORITY =
            "com.example.vpngateviewer.fileprovider";
    private static final String VPN_PROFILES_DIR = "vpn_profiles";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView errorText;
    private CountryPagerAdapter countryPagerAdapter;
    private Set<String> cachedServerKeys;
    private Set<String> favoriteServerKeys;
    private final AtomicBoolean isFetching = new AtomicBoolean(false);
    private final Runnable refreshRunnable = this::refreshVpnData;
    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "vpn-fetch-thread");
                t.setDaemon(true);
                return t;
            });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VpnGateClient vpnGateClient = new VpnGateClient();

    // ---- lifecycle ----

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);

        Set<String> emptySet = new HashSet<>();
        cachedServerKeys = new HashSet<>(
                getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                        .getStringSet(KEY_CACHED_SERVERS, emptySet));
        favoriteServerKeys = new HashSet<>(
                getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                        .getStringSet(KEY_FAVORITE_SERVERS, emptySet));

        countryPagerAdapter = new CountryPagerAdapter(this);
        viewPager.setAdapter(countryPagerAdapter);

        fetchVpnData(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleNextRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(refreshRunnable);
        executorService.shutdownNow();
    }

    // ---- public API for fragments ----

    @Nullable
    public CountryPagerAdapter getCountryPagerAdapter() {
        return countryPagerAdapter;
    }

    // ---- data fetching ----

    private void refreshVpnData() {
        if (isFetching.get()) {
            scheduleNextRefresh();
            return;
        }
        fetchVpnData(false);
    }

    private void fetchVpnData(boolean showLoading) {
        if (!isFetching.compareAndSet(false, true)) {
            Log.d(TAG, "Skipping fetch — already in progress");
            return;
        }

        if (showLoading) {
            progressBar.setVisibility(View.VISIBLE);
            errorText.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
        }

        executorService.execute(() -> {
            List<VpnServer> servers;
            try {
                servers = vpnGateClient.fetchVpnServers();
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch VPN servers", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    errorText.setText(getString(R.string.error_load_failed));
                    errorText.setVisibility(View.VISIBLE);
                    scheduleNextRefresh();
                    isFetching.set(false);
                });
                return;
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (servers.isEmpty()) {
                    errorText.setText(getString(R.string.error_no_servers));
                    errorText.setVisibility(View.VISIBLE);
                } else {
                    setupCountryTabs(servers);
                    viewPager.setVisibility(View.VISIBLE);
                }
                scheduleNextRefresh();
                isFetching.set(false);
            });
        });
    }

    private void setupCountryTabs(List<VpnServer> servers) {
        Map<String, List<VpnServer>> groupedByCountry = new LinkedHashMap<>();
        Set<String> nextCachedKeys = new HashSet<>();

        for (VpnServer server : servers) {
            String countryCode = server.getCountryShort();
            String cacheKey = server.getCacheKey();

            server.setNewlyAdded(!cachedServerKeys.contains(cacheKey));
            server.setFavorite(favoriteServerKeys.contains(cacheKey));
            nextCachedKeys.add(cacheKey);

            groupedByCountry
                    .computeIfAbsent(countryCode, k -> new ArrayList<>())
                    .add(server);
        }

        List<CountryTab> countryTabs = new ArrayList<>();
        for (Map.Entry<String, List<VpnServer>> entry : groupedByCountry.entrySet()) {
            List<VpnServer> sortedServers = new ArrayList<>(entry.getValue());
            Collections.sort(sortedServers, Comparator
                    .comparing(VpnServer::isNewlyAdded, Comparator.reverseOrder())
                    .thenComparing(VpnServer::isFavorite, Comparator.reverseOrder())
                    .thenComparingInt(VpnServer::getPing));

            String countryCode = entry.getKey();
            String countryName = sortedServers.get(0).getCountryLong();
            countryTabs.add(new CountryTab(countryName, countryCode, sortedServers));
        }

        // Place US first, then alphabetical
        Collections.sort(countryTabs, (a, b) -> {
            if ("US".equalsIgnoreCase(a.getCountryCode())) return -1;
            if ("US".equalsIgnoreCase(b.getCountryCode())) return 1;
            return a.getCountryName().compareTo(b.getCountryName());
        });

        countryPagerAdapter.setCountryTabs(countryTabs);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(countryPagerAdapter.getPageTitle(position))
        ).attach();

        cachedServerKeys = nextCachedKeys;
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_CACHED_SERVERS, new HashSet<>(cachedServerKeys))
                .apply();
    }

    // ---- periodic refresh ----

    private void scheduleNextRefresh() {
        mainHandler.removeCallbacks(refreshRunnable);
        mainHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    // ---- VPN connect ----

    private void onConnectClick(VpnServer server) {
        String base64Config = server.getOpenVPNConfigDataBase64();
        if (base64Config == null || base64Config.isEmpty()) {
            Toast.makeText(this, R.string.error_no_config, Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                byte[] configData = Base64.decode(base64Config, Base64.DEFAULT);

                File cacheDir = new File(getCacheDir(), VPN_PROFILES_DIR);
                if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                    mainHandler.post(() -> Toast.makeText(MainActivity.this,
                            R.string.error_file_creation, Toast.LENGTH_SHORT).show());
                    return;
                }

                String baseFilename = "vpngate_" + server.getIp().replace(".", "_");
                File configFile = new File(cacheDir, baseFilename + ".ovpn");
                if (configFile.exists()) {
                    configFile = new File(cacheDir,
                            baseFilename + "_" + System.currentTimeMillis() + ".ovpn");
                }

                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    fos.write(configData);
                }

                Uri contentUri = FileProvider.getUriForFile(
                        this, FILE_PROVIDER_AUTHORITY, configFile);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/x-openvpn-profile");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                mainHandler.post(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.w(TAG, "No OpenVPN app found", e);
                        Toast.makeText(MainActivity.this,
                                R.string.error_no_openvpn_app,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Error writing VPN config", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        R.string.error_file_creation, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ---- listener callbacks ----

    @Override
    public void onServerClick(VpnServer server) {
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
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_FAVORITE_SERVERS, new HashSet<>(favoriteServerKeys))
                .apply();
        Toast.makeText(this,
                server.isFavorite() ? R.string.toast_favorite_added
                                    : R.string.toast_favorite_removed,
                Toast.LENGTH_SHORT).show();
    }
}