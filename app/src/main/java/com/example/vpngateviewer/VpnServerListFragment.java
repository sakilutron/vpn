package com.example.vpngateviewer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Fragment displaying VPN servers for a single country tab.
 * <p>
 * Receives only a tab-position index via arguments and looks up data from
 * the parent {@link CountryPagerAdapter} — avoiding Java {@code Serializable}
 * overhead that would serialize the entire server list into the Bundle.
 */
public class VpnServerListFragment extends Fragment {

    private static final String ARG_TAB_INDEX = "tab_index";

    private List<VpnServer> vpnServers = Collections.emptyList();
    private String countryName = "";
    private String countryCode = "";
    private OnVpnServerClickListener listener;
    private VpnAdapter vpnAdapter;

    public interface OnVpnServerClickListener extends VpnAdapter.OnItemClickListener {}

    /**
     * Creates a new instance keyed by tab position.
     * Data is resolved at creation time from the owning CountryPagerAdapter.
     */
    static VpnServerListFragment newInstance(int tabIndex) {
        VpnServerListFragment fragment = new VpnServerListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_INDEX, tabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnVpnServerClickListener) {
            listener = (OnVpnServerClickListener) context;
        } else {
            throw new IllegalStateException(
                    "Hosting activity must implement OnVpnServerClickListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        int tabIndex = args.getInt(ARG_TAB_INDEX, -1);
        if (tabIndex < 0) {
            return;
        }

        // Resolve data from the hosting activity's adapter to avoid Serializable
        Context ctx = getContext();
        if (ctx instanceof MainActivity) {
            CountryPagerAdapter adapter =
                    ((MainActivity) ctx).getCountryPagerAdapter();
            if (adapter != null && tabIndex < adapter.getItemCount()) {
                CountryTab tab = adapter.getCountryTab(tabIndex);
                if (tab != null) {
                    vpnServers = tab.getServers();
                    countryName = tab.getCountryName();
                    countryCode = tab.getCountryCode();
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vpn_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.vpnRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        vpnAdapter = new VpnAdapter(listener);
        recyclerView.setAdapter(vpnAdapter);
        vpnAdapter.submitList(vpnServers);
    }

    /** Returns a display label with the country flag emoji. */
    public String getCountryTitleWithFlag() {
        String flag = CountryFlagUtils.countryCodeToFlag(countryCode);
        return (flag.isEmpty() ? "" : flag + " ") + countryName;
    }
}