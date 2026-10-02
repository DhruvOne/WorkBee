package com.workbee.app.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Provider;
import com.workbee.app.ui.adapter.ProviderAdapter;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ProviderListActivity extends BaseActivity {

    private Toolbar toolbar;
    private RecyclerView recycler;
    private LinearLayout layoutEmpty;
    private ProviderAdapter adapter;
    private String categoryName;
    private final List<Provider> providerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_list);

        toolbar = findViewById(R.id.toolbar);
        recycler = findViewById(R.id.recycler_providers_list);
        layoutEmpty = findViewById(R.id.layout_empty_state);

        categoryName = getIntent().getStringExtra("category_name");
        if (categoryName == null) categoryName = "Plumbing";

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(categoryName + " Experts");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recycler.setLayoutManager(new LinearLayoutManager(this));

        loadProviders();
    }

    private void loadProviders() {
        showProgress("Searching local professionals...");
        repository.getProviders(categoryName, new FirebaseHelper.ListCallback<Provider>() {
            @Override
            public void onSuccess(List<Provider> list) {
                hideProgress();
                providerList.clear();
                // Load approved providers only
                for (Provider p : list) {
                    if (p.isApproved()) {
                        providerList.add(p);
                    }
                }

                if (providerList.isEmpty()) {
                    recycler.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    recycler.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);

                    adapter = new ProviderAdapter(ProviderListActivity.this, providerList, prov -> {
                        Intent intent = new Intent(ProviderListActivity.this, ProviderDetailsActivity.class);
                        intent.putExtra("provider_id", prov.getProviderId());
                        startActivity(intent);
                    });
                    recycler.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                Toast.makeText(ProviderListActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
