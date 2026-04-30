package com.example.vpngateviewer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Collections;
import java.util.List;

/**
 * ViewPager2 adapter for country-tabbed VPN server lists.
 * <p>
 * Fragments are identified by position only (no Serializable data in Bundles),
 * and data is resolved from this adapter's internal {@link CountryTab} list.
 */
public class CountryPagerAdapter extends FragmentStateAdapter {

    private List<CountryTab> countryTabs = Collections.emptyList();

    public CountryPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Replaces the data backing this adapter and notifies the ViewPager2.
     * Fragments will be recreated on the next bind.
     */
    public void setCountryTabs(@NonNull List<CountryTab> tabs) {
        this.countryTabs = tabs;
        notifyDataSetChanged();
    }

    /** Returns the CountryTab at the given index, or null if out of bounds. */
    @Nullable
    public CountryTab getCountryTab(int position) {
        if (position < 0 || position >= countryTabs.size()) {
            return null;
        }
        return countryTabs.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return VpnServerListFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return countryTabs.size();
    }

    /** Returns the flag + country name for the tab label. */
    @NonNull
    public String getPageTitle(int position) {
        CountryTab tab = countryTabs.get(position);
        return tab != null ? tab.getTitleWithFlag() : "";
    }
}