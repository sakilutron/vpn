package com.example.vpngateviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

/**
 * RecyclerView adapter for VPN server list items.
 * <p>
 * Uses {@link ListAdapter} with {@link DiffUtil} for efficient incremental
 * updates instead of {@code notifyDataSetChanged()}.
 */
public class VpnAdapter extends ListAdapter<VpnServer, VpnAdapter.VpnViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onServerClick(VpnServer server);
        void onFavoriteToggle(VpnServer server);
    }

    private static final DiffUtil.ItemCallback<VpnServer> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VpnServer>() {
                @Override
                public boolean areItemsTheSame(@NonNull VpnServer oldItem,
                                               @NonNull VpnServer newItem) {
                    return oldItem.getCacheKey().equals(newItem.getCacheKey());
                }

                @Override
                public boolean areContentsTheSame(@NonNull VpnServer oldItem,
                                                  @NonNull VpnServer newItem) {
                    return oldItem.isFavorite() == newItem.isFavorite()
                            && oldItem.isNewlyAdded() == newItem.isNewlyAdded()
                            && oldItem.getPing() == newItem.getPing()
                            && oldItem.getSpeed() == newItem.getSpeed();
                }

                @Override
                public Object getChangePayload(@NonNull VpnServer oldItem,
                                               @NonNull VpnServer newItem) {
                    return new VpnDiffPayload(
                            oldItem.isFavorite() != newItem.isFavorite(),
                            oldItem.isNewlyAdded() != newItem.isNewlyAdded(),
                            oldItem.getPing() != newItem.getPing(),
                            oldItem.getSpeed() != newItem.getSpeed()
                    );
                }
            };

    private static class VpnDiffPayload {
        final boolean favoriteChanged;
        final boolean newBadgeChanged;
        final boolean pingChanged;
        final boolean speedChanged;

        VpnDiffPayload(boolean favoriteChanged, boolean newBadgeChanged,
                       boolean pingChanged, boolean speedChanged) {
            this.favoriteChanged = favoriteChanged;
            this.newBadgeChanged = newBadgeChanged;
            this.pingChanged = pingChanged;
            this.speedChanged = speedChanged;
        }

        boolean hasChanged() {
            return favoriteChanged || newBadgeChanged || pingChanged || speedChanged;
        }
    }

    public VpnAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VpnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vpn_server, parent, false);
        return new VpnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VpnViewHolder holder, int position) {
        holder.bind(getItem(position), listener, null);
    }

    @Override
    public void onBindViewHolder(@NonNull VpnViewHolder holder, int position,
                                 @NonNull java.util.List<Object> payloads) {
        VpnServer server = getItem(position);
        if (payloads.isEmpty()) {
            holder.bind(server, listener, null);
            return;
        }

        VpnDiffPayload payload = (VpnDiffPayload) payloads.get(0);
        if (!payload.hasChanged()) {
            holder.bind(server, listener, null);
            return;
        }

        if (payload.favoriteChanged) {
            holder.favoriteToggle.setText(server.isFavorite() ? "\u2605" : "\u2606");
        }
        if (payload.newBadgeChanged) {
            holder.newBadge.setVisibility(server.isNewlyAdded() ? View.VISIBLE : View.GONE);
        }
        if (payload.pingChanged) {
            holder.ping.setText("Ping: " + server.getPing() + " ms");
        }
        if (payload.speedChanged) {
            double speedMbps = server.getSpeed() / 1_000_000.0;
            holder.speed.setText(String.format(Locale.getDefault(),
                    "Speed: %.2f Mbps", speedMbps));
        }
    }

    static class VpnViewHolder extends RecyclerView.ViewHolder {
        final TextView countryFlag;
        final TextView country;
        final TextView ipAddress;
        final TextView speed;
        final TextView ping;
        final TextView favoriteToggle;
        final View newBadge;

        VpnViewHolder(@NonNull View itemView) {
            super(itemView);
            countryFlag = itemView.findViewById(R.id.country_flag);
            country = itemView.findViewById(R.id.country);
            ipAddress = itemView.findViewById(R.id.ip_address);
            speed = itemView.findViewById(R.id.speed);
            ping = itemView.findViewById(R.id.ping);
            favoriteToggle = itemView.findViewById(R.id.favorite_toggle);
            newBadge = itemView.findViewById(R.id.new_badge);
        }

        void bind(VpnServer server, OnItemClickListener listener,
                  VpnDiffPayload payload) {
            if (payload == null || payload.hasChanged()) {
                countryFlag.setText(CountryFlagUtils.countryCodeToFlag(server.getCountryShort()));
            }
            country.setText(server.getCountryLong() + " (" + server.getCountryShort() + ")");
            ipAddress.setText("IP: " + server.getIp());

            double speedMbps = server.getSpeed() / 1_000_000.0;
            speed.setText(String.format(Locale.getDefault(),
                    "Speed: %.2f Mbps", speedMbps));
            ping.setText("Ping: " + server.getPing() + " ms");

            itemView.setOnClickListener(v -> listener.onServerClick(server));

            favoriteToggle.setText(server.isFavorite() ? "\u2605" : "\u2606");
            favoriteToggle.setOnClickListener(v -> {
                server.setFavorite(!server.isFavorite());
                favoriteToggle.setText(server.isFavorite() ? "\u2605" : "\u2606");
                listener.onFavoriteToggle(server);
            });

            newBadge.setVisibility(server.isNewlyAdded() ? View.VISIBLE : View.GONE);
        }
    }
}