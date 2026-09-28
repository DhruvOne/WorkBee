package com.workbee.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<Category> categories;
    private final Context context;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category cat = categories.get(position);
        holder.name.setText(cat.getName());

        // Dynamic icon loading based on name
        String iconName = cat.getIconName();
        if (iconName != null && iconName.startsWith("emoji:")) {
            holder.icon.setVisibility(View.GONE);
            holder.txtEmoji.setVisibility(View.VISIBLE);
            holder.txtEmoji.setText(iconName.substring(6));
        } else {
            holder.icon.setVisibility(View.VISIBLE);
            holder.txtEmoji.setVisibility(View.GONE);
            int iconRes = R.drawable.ic_workbee_logo; // Default fallback
            if (iconName != null) {
                int checkRes = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
                if (checkRes != 0) {
                    iconRes = checkRes;
                }
            }
            holder.icon.setImageResource(iconRes);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(cat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView txtEmoji;
        TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_cat_icon);
            txtEmoji = itemView.findViewById(R.id.txt_cat_emoji);
            name = itemView.findViewById(R.id.txt_cat_name);
        }
    }
}
