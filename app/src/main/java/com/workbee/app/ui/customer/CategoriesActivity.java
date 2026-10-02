package com.workbee.app.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Category;
import com.workbee.app.ui.adapter.CategoryAdapter;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;

import java.util.List;

public class CategoriesActivity extends BaseActivity {

    private Toolbar toolbar;
    private RecyclerView recycler;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        toolbar = findViewById(R.id.toolbar);
        recycler = findViewById(R.id.recycler_all_categories);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recycler.setLayoutManager(new GridLayoutManager(this, 3));

        loadCategories();
    }

    private void loadCategories() {
        showProgress("Loading service categories...");
        repository.getCategories(new FirebaseHelper.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> list) {
                hideProgress();
                adapter = new CategoryAdapter(CategoriesActivity.this, list, cat -> {
                    Intent intent = new Intent(CategoriesActivity.this, BookingActivity.class);
                    intent.putExtra("category_name", cat.getName());
                    intent.putExtra("category_rate", cat.getBasePrice());
                    startActivity(intent);
                });
                recycler.setAdapter(adapter);
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                Toast.makeText(CategoriesActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
