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

import java.util.ArrayList;
import java.util.List;

public class VpnServerListFragment extends Fragment {

    private static final String ARG_COUNTRY_NAME = "arg_country_name";
    private static final String ARG_COUNTRY_CODE = "arg_country_code";
    private static final String ARG_SERVERS = "arg_servers";

    private List<VpnServer> vpnServers = new ArrayList<>();
    private String countryName;
    private String countryCode;
    private VpnAdapter.OnItemClickListener listener;

    public interface OnVpnServerClickListener extends VpnAdapter.OnItemClickListener {}

    public static VpnServerListFragment newInstance(String countryName, String countryCode, ArrayList<VpnServer> servers) {
        VpnServerListFragment fragment = new VpnServerListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COUNTRY_NAME, countryName);
        args.putString(ARG_COUNTRY_CODE, countryCode);
        args.putSerializable(ARG_SERVERS, servers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnVpnServerClickListener) {
            listener = (OnVpnServerClickListener) context;
        } else {
            throw new IllegalStateException("Hosting activity must implement OnVpnServerClickListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            countryName = args.getString(ARG_COUNTRY_NAME);
            countryCode = args.getString(ARG_COUNTRY_CODE);
            Object servers = args.getSerializable(ARG_SERVERS);
            if (servers instanceof ArrayList) {
                //noinspection unchecked
                vpnServers = (ArrayList<VpnServer>) servers;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vpn_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.vpnRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        VpnAdapter vpnAdapter = new VpnAdapter(vpnServers, listener);
        recyclerView.setAdapter(vpnAdapter);
    }

    public String getCountryTitleWithFlag() {
        String flag = CountryFlagUtils.countryCodeToFlag(countryCode);
        return (flag.isEmpty() ? "" : flag + " ") + countryName;
    }
}
