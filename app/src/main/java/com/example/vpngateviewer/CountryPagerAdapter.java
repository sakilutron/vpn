package com.example.vpngateviewer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class CountryPagerAdapter extends FragmentStateAdapter {

    private final List<CountryTab> countryTabs;

    public CountryPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<CountryTab> countryTabs) {
        super(fragmentActivity);
        this.countryTabs = countryTabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        CountryTab tab = countryTabs.get(position);
        return VpnServerListFragment.newInstance(tab.getCountryName(), tab.getCountryCode(), new java.util.ArrayList<>(tab.getServers()));
    }

    @Override
    public int getItemCount() {
        return countryTabs != null ? countryTabs.size() : 0;
    }

    public String getPageTitle(int position) {
        return countryTabs.get(position).getTitleWithFlag();
    }
}
