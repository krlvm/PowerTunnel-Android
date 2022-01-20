/*
 * This file is part of PowerTunnel-Android.
 *
 * PowerTunnel-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.android.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.adapters.AppAdapter;
import io.github.krlvm.powertunnel.android.databinding.ListActivityBinding;
import io.github.krlvm.powertunnel.android.types.AppInfo;

public class AppListActivity extends AppCompatActivity {

    private ListActivityBinding binding;

    private String preferenceKey;
    private AppAdapter appAdapter;
    private AppInfo.FilterCallback appFilter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(getIntent().getExtras().getInt("title")));
        preferenceKey = getIntent().getExtras().getString("key");

        binding = ListActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.appList.setLayoutManager(new LinearLayoutManager(this));
        binding.appList.addItemDecoration(new DividerItemDecoration(
                binding.appList.getContext(),
                RecyclerView.VERTICAL
        ));
        resetAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_list, menu);

        final MenuItem mShowCheckedItem = menu.findItem(R.id.action_show_checked);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final String _query = query.toLowerCase();
                setFilter(app -> app.label.toLowerCase().contains(_query)
                        || app.packageName.toLowerCase().contains(_query));
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) { return false; }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) { return true; }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mShowCheckedItem.setChecked(false);
                setFilter(null);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == android.R.id.home) {
            super.onBackPressed();
        } else if (id == R.id.action_show_checked) {
            item.setChecked(!item.isChecked());
            if (item.isChecked()) {
                setFilter(app -> app.checked);
            } else {
                setFilter(null);
            }
            return true;
        } else if (id == R.id.action_refresh) {
            resetAdapter();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setFilter(AppInfo.FilterCallback filter) {
        appFilter = filter;
        if(appAdapter != null) {
            appAdapter.filtrate(appFilter);
        }
    }

    private void resetAdapter() {
        binding.appList.setVisibility(View.GONE);
        binding.progressCircular.setVisibility(View.VISIBLE);
        new Thread(() -> {
            final List<AppInfo> apps = AppInfo.getInstalledApps(this, preferenceKey);
            runOnUiThread(() -> {
                appAdapter = new AppAdapter(this, preferenceKey, apps);
                binding.appList.setAdapter(appAdapter);
                appAdapter.filtrate(appFilter);

                binding.progressCircular.setVisibility(View.GONE);
                binding.appList.setVisibility(View.VISIBLE);
            });
        }).start();
    }
}
