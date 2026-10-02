package com.workbee.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Provider;

import java.util.List;
import java.util.Locale;

public class AdminVettingAdapter extends RecyclerView.Adapter<AdminVettingAdapter.ViewHolder> {

    private final Context context;
    private final List<Provider> providerList;
    private final OnVettingActionListener listener;

    public interface OnVettingActionListener {
        void onApprove(Provider provider);
        void onSuspend(Provider provider);
    }

    public AdminVettingAdapter(Context context, List<Provider> providerList, OnVettingActionListener listener) {
        this.context = context;
        this.providerList = providerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_provider_approval, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Provider p = providerList.get(position);

        holder.txtBusinessName.setText(p.getBusinessName());
        holder.txtRate.setText(String.format(Locale.getDefault(), "$%.2f/hr", p.getHourlyRate()));
        holder.txtCategory.setText(p.getCategory());
        holder.txtExperience.setText("Experience: " + p.getExperienceYears() + " Years");
        holder.txtBio.setText(p.getBio());
        holder.txtStats.setText(String.format(Locale.getDefault(), "Rating: %.1f ★ | Completed: %d jobs", p.getRating(), p.getCompletedJobs()));

        if (p.isApproved()) {
            holder.txtStatusPill.setText("ACTIVE PARTNER");
            holder.txtStatusPill.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_status_completed));
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnSuspend.setVisibility(View.VISIBLE);
            holder.btnSuspend.setText("Suspend / Block");
        } else {
            holder.txtStatusPill.setText("AWAITING VETTING");
            holder.txtStatusPill.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_status_pending));
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnSuspend.setVisibility(View.VISIBLE);
            holder.btnSuspend.setText("Reject Applicant");
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(p);
            }
        });

        holder.btnSuspend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuspend(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return providerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtBusinessName, txtRate, txtCategory, txtExperience, txtBio, txtStats, txtStatusPill;
        Button btnApprove, btnSuspend;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBusinessName = itemView.findViewById(R.id.txt_vet_business_name);
            txtRate = itemView.findViewById(R.id.txt_vet_rate);
            txtCategory = itemView.findViewById(R.id.txt_vet_category);
            txtExperience = itemView.findViewById(R.id.txt_vet_experience);
            txtBio = itemView.findViewById(R.id.txt_vet_bio);
            txtStats = itemView.findViewById(R.id.txt_vet_stats);
            txtStatusPill = itemView.findViewById(R.id.txt_vet_status_pill);
            btnApprove = itemView.findViewById(R.id.btn_vet_approve);
            btnSuspend = itemView.findViewById(R.id.btn_vet_suspend);
        }
    }
}
