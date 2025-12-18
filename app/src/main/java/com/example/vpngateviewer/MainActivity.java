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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView errorText;
    private VpnAdapter adapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final VpnGateClient vpnGateClient = new VpnGateClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VpnAdapter(new ArrayList<>(), this::onConnectClick);
        recyclerView.setAdapter(adapter);

        fetchVpnData();
    }

    private void fetchVpnData() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        executorService.execute(() -> {
            try {
                List<VpnServer> servers = vpnGateClient.fetchVpnServers();
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (servers.isEmpty()) {
                        errorText.setText("No VPN servers found.");
                        errorText.setVisibility(View.VISIBLE);
                    } else {
                        adapter.updateData(servers);
                        recyclerView.setVisibility(View.VISIBLE);
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
            // Use a safe filename
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
}
