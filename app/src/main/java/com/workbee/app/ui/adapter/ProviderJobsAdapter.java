package com.workbee.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;

import java.util.List;
import java.util.Locale;

public class ProviderJobsAdapter extends RecyclerView.Adapter<ProviderJobsAdapter.ViewHolder> {

    private final Context context;
    private final List<Booking> jobs;
    private final OnJobActionListener listener;

    public interface OnJobActionListener {
        void onUpdateJobStatus(Booking booking, String nextStatus);
    }

    public ProviderJobsAdapter(Context context, List<Booking> jobs, OnJobActionListener listener) {
        this.context = context;
        this.jobs = jobs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_provider_job, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking b = jobs.get(position);

        holder.txtCategory.setText(b.getCategory() + " Service");
        holder.txtPrice.setText(String.format(Locale.getDefault(), "₹%.2f", b.getTotalPrice()));
        holder.txtCustomer.setText("Customer: " + b.getCustomerName());
        holder.txtDesc.setText(b.getDescription());
        holder.txtAddress.setText(b.getAddress());
        holder.txtDate.setText("Date: " + b.getDate());
        holder.txtSlot.setText("Time: " + b.getTimeSlot());

        String payMethod = b.getPaymentMethod();
        if (payMethod == null || payMethod.equalsIgnoreCase("CASH")) {
            holder.txtPaymentMethod.setText("Payment: 💵 Cash on Service");
        } else {
            holder.txtPaymentMethod.setText("Payment: 💳 Paid Online");
        }

        String status = b.getStatus().toUpperCase();
        holder.txtStatus.setText(status);

        // Styling status pill
        int bgDrawable;
        if (status.equals("COMPLETED")) {
            bgDrawable = R.drawable.bg_status_completed;
        } else if (status.equals("PENDING")) {
            bgDrawable = R.drawable.bg_status_pending;
        } else if (status.equals("CANCELLED") || status.equals("REJECTED")) {
            bgDrawable = R.drawable.bg_status_cancelled;
        } else if (status.equals("ON_THE_WAY")) {
            bgDrawable = R.drawable.bg_status_pending; // Yellow/Amber for transit
        } else if (status.equals("IN_PROGRESS")) {
            bgDrawable = R.drawable.bg_status_active; // Blue/Active for in-progress
        } else {
            bgDrawable = R.drawable.bg_status_active; // Blue for ACCEPTED
        }
        holder.txtStatus.setBackground(ContextCompat.getDrawable(context, bgDrawable));

        // Multi-Stage Uber progress workflow logic
        if (status.equals("ACCEPTED")) {
            holder.txtActionSubtitle.setVisibility(View.VISIBLE);
            holder.txtActionSubtitle.setText("Next Stage: Head over to client's address 🚗");
            holder.txtActionSubtitle.setTextColor(ContextCompat.getColor(context, R.color.primary));

            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Start Driving to Client 🚗");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUpdateJobStatus(b, "ON_THE_WAY");
                }
            });
        } else if (status.equals("ON_THE_WAY")) {
            holder.txtActionSubtitle.setVisibility(View.VISIBLE);
            holder.txtActionSubtitle.setText("Next Stage: Arrived at client's door. Start task 🛠️");
            holder.txtActionSubtitle.setTextColor(ContextCompat.getColor(context, R.color.status_in_progress));

            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Begin Service / Work 🛠️");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_in_progress)));
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUpdateJobStatus(b, "IN_PROGRESS");
                }
            });
        } else if (status.equals("IN_PROGRESS")) {
            holder.txtActionSubtitle.setVisibility(View.VISIBLE);
            holder.txtActionSubtitle.setText("Next Stage: Complete the service and claim payout ✓");
            holder.txtActionSubtitle.setTextColor(ContextCompat.getColor(context, R.color.status_completed));

            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Mark Job Completed ✓");
            holder.btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_completed)));
            holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUpdateJobStatus(b, "COMPLETED");
                }
            });
        } else {
            // Hide for other statuses (e.g. COMPLETED, REJECTED, PENDING, CANCELLED)
            holder.txtActionSubtitle.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory, txtPrice, txtCustomer, txtDesc, txtAddress, txtDate, txtSlot, txtStatus, txtPaymentMethod, txtActionSubtitle;
        Button btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txt_job_category);
            txtPrice = itemView.findViewById(R.id.txt_job_price);
            txtCustomer = itemView.findViewById(R.id.txt_job_customer);
            txtDesc = itemView.findViewById(R.id.txt_job_desc);
            txtAddress = itemView.findViewById(R.id.txt_job_address);
            txtDate = itemView.findViewById(R.id.txt_job_date);
            txtSlot = itemView.findViewById(R.id.txt_job_slot);
            txtStatus = itemView.findViewById(R.id.txt_job_status);
            txtPaymentMethod = itemView.findViewById(R.id.txt_job_payment_method);
            txtActionSubtitle = itemView.findViewById(R.id.txt_job_action_subtitle);
            btnAction = itemView.findViewById(R.id.btn_action_job);
        }
    }
}
