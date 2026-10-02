package com.workbee.app.ui.admin.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.workbee.app.R;
import com.workbee.app.data.model.Category;
import com.workbee.app.ui.adapter.CategoryAdapter;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class AdminCategoriesFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView recyclerCategories;
    private FloatingActionButton fabAdd;

    private FirebaseHelper firebaseHelper;
    private final List<Category> categoriesList = new ArrayList<>();
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_categories, container, false);

        firebaseHelper = FirebaseHelper.getInstance(requireContext());

        recyclerCategories = view.findViewById(R.id.recycler_admin_categories);
        fabAdd = view.findViewById(R.id.fab_add_category);

        recyclerCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new CategoryAdapter(requireContext(), categoriesList, this);
        recyclerCategories.setAdapter(adapter);

        loadCategories();

        fabAdd.setOnClickListener(v -> showAddCategoryDialog());

        return view;
    }

    private void loadCategories() {
        firebaseHelper.getCategories(new FirebaseHelper.ListCallback<Category>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(List<Category> list) {
                categoriesList.clear();
                categoriesList.addAll(list);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to load categories: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        EditText edtName = dialogView.findViewById(R.id.dialog_edt_cat_name);
        EditText edtDesc = dialogView.findViewById(R.id.dialog_edt_cat_desc);
        EditText edtPrice = dialogView.findViewById(R.id.dialog_edt_cat_price);
        EditText edtIcon = dialogView.findViewById(R.id.dialog_edt_cat_icon);

        builder.setPositiveButton("Add Service", null); // Override later to prevent auto-closing on invalid input
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Custom validation click listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            String icon = edtIcon.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                edtName.setError("Category Name is required");
                return;
            }
            if (TextUtils.isEmpty(desc)) {
                edtDesc.setError("Description is required");
                return;
            }
            if (TextUtils.isEmpty(priceStr)) {
                edtPrice.setError("Base Price is required");
                return;
            }

            double basePrice;
            try {
                basePrice = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                edtPrice.setError("Enter a valid price");
                return;
            }

            if (TextUtils.isEmpty(icon)) {
                icon = "ic_workbee_logo";
            }

            String catId = "cat_" + name.toLowerCase().replace(" ", "_");
            Category newCategory = new Category(catId, name, desc, basePrice, icon);

            firebaseHelper.addCategory(newCategory, new FirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), name + " successfully registered!", Toast.LENGTH_LONG).show();
                    loadCategories(); // Reload list
                    dialog.dismiss();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(getContext(), "Registration failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onCategoryClick(Category category) {
        // Detailed description dialog
        new AlertDialog.Builder(requireContext())
                .setTitle(category.getName())
                .setMessage(category.getDescription() + "\n\nBase Price: $" + category.getBasePrice() + "/hr")
                .setPositiveButton("Close", null)
                .show();
    }
}
