package com.example.vpngateviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vpngateviewer.CountryFlagUtils;

import java.util.List;
import java.util.Locale;

public class VpnAdapter extends RecyclerView.Adapter<VpnAdapter.VpnViewHolder> {

    private List<VpnServer> vpnServerList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(VpnServer server);
    }

    public VpnAdapter(List<VpnServer> vpnServerList, OnItemClickListener listener) {
        this.vpnServerList = vpnServerList;
        this.listener = listener;
    }

    public void updateData(List<VpnServer> newVpnServerList) {
        this.vpnServerList = newVpnServerList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VpnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vpn_server, parent, false);
        return new VpnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VpnViewHolder holder, int position) {
        VpnServer server = vpnServerList.get(position);
        holder.bind(server, listener);
    }

    @Override
    public int getItemCount() {
        return vpnServerList != null ? vpnServerList.size() : 0;
    }

    static class VpnViewHolder extends RecyclerView.ViewHolder {
        TextView countryFlag;
        TextView country;
        TextView ipAddress;
        TextView speed;
        TextView ping;

        public VpnViewHolder(@NonNull View itemView) {
            super(itemView);
            countryFlag = itemView.findViewById(R.id.country_flag);
            country = itemView.findViewById(R.id.country);
            ipAddress = itemView.findViewById(R.id.ip_address);
            speed = itemView.findViewById(R.id.speed);
            ping = itemView.findViewById(R.id.ping);
        }

        public void bind(final VpnServer server, final OnItemClickListener listener) {
            countryFlag.setText(CountryFlagUtils.countryCodeToFlag(server.getCountryShort()));
            country.setText(server.getCountryLong() + " (" + server.getCountryShort() + ")");
            ipAddress.setText("IP: " + server.getIp());
            
            // Speed is in bps, convert to Mbps
            double speedMbps = server.getSpeed() / 1000000.0;
            speed.setText(String.format(Locale.getDefault(), "Speed: %.2f Mbps", speedMbps));
            
            ping.setText("Ping: " + server.getPing() + " ms");

            itemView.setOnClickListener(v -> listener.onItemClick(server));
        }
    }
}
