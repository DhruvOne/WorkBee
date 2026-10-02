package com.workbee.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

    private final int[] slideIcons = {
        R.drawable.ic_workbee_logo,
        R.drawable.ic_workbee_logo,
        R.drawable.ic_workbee_logo
    };

    private final int[] slideTitles = {
        R.string.onboarding_title_1,
        R.string.onboarding_title_2,
        R.string.onboarding_title_3
    };

    private final int[] slideDescs = {
        R.string.onboarding_desc_1,
        R.string.onboarding_desc_2,
        R.string.onboarding_desc_3
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.icon.setImageResource(slideIcons[position]);
        holder.title.setText(slideTitles[position]);
        holder.desc.setText(slideDescs[position]);
    }

    @Override
    public int getItemCount() {
        return slideTitles.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView desc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_slide);
            title = itemView.findViewById(R.id.txt_title);
            desc = itemView.findViewById(R.id.txt_desc);
        }
    }
}
