package com.workbee.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.utils.FirebaseHelper;

import java.util.List;
import java.util.Locale;

public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ViewHolder> {

    private final List<Provider> providers;
    private final Context context;
    private final OnProviderClickListener listener;
    private final FirebaseHelper firebaseHelper;

    public interface OnProviderClickListener {
        void onProviderClick(Provider provider);
    }

    public ProviderAdapter(Context context, List<Provider> providers, OnProviderClickListener listener) {
        this.context = context;
        this.providers = providers;
        this.listener = listener;
        this.firebaseHelper = FirebaseHelper.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_provider, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Provider p = providers.get(position);

        holder.txtCategory.setText(p.getCategory().toUpperCase());
        holder.txtBusiness.setText(p.getBusinessName());
        holder.txtRating.setText(String.format(Locale.getDefault(), " %.1f", p.getRating()));
        holder.txtReviews.setText(" (" + p.getReviewCount() + " reviews)");
        holder.txtExperience.setText(p.getExperienceYears() + " yrs exp");
        holder.txtRate.setText(String.format(Locale.getDefault(), "₹%.0f", p.getHourlyRate()));

        // Display pre-seeded names based on provider UIDs
        firebaseHelper.fetchUserProfile(p.getProviderId(), new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                holder.txtName.setText(user.getFullName());
                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(context)
                         .load(user.getProfileImageUrl())
                         .placeholder(R.drawable.ic_workbee_logo)
                         .into(holder.imgAvatar);
                } else {
                    holder.imgAvatar.setImageResource(R.drawable.ic_workbee_logo);
                }
            }

            @Override
            public void onFailure(String message) {
                holder.txtName.setText("Vetted Professional");
                holder.imgAvatar.setImageResource(R.drawable.ic_workbee_logo);
            }
        });

        // Compute simulated dynamic distance based on index to show realistic mock maps features
        double distance = 0.5 + (position * 0.4);
        holder.txtDistance.setText(String.format(Locale.getDefault(), "%.1f mi", distance));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProviderClick(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtCategory, txtBusiness, txtName, txtRating, txtReviews, txtExperience, txtRate, txtDistance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_provider);
            txtCategory = itemView.findViewById(R.id.txt_provider_category);
            txtBusiness = itemView.findViewById(R.id.txt_provider_business);
            txtName = itemView.findViewById(R.id.txt_provider_name);
            txtRating = itemView.findViewById(R.id.txt_provider_rating);
            txtReviews = itemView.findViewById(R.id.txt_provider_reviews);
            txtExperience = itemView.findViewById(R.id.txt_provider_experience);
            txtRate = itemView.findViewById(R.id.txt_provider_rate);
            txtDistance = itemView.findViewById(R.id.txt_provider_distance);
        }
    }
}
