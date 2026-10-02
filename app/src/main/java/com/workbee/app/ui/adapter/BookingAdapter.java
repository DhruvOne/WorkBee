package com.workbee.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;

import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final List<Booking> bookings;
    private final Context context;
    private final OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onRateClick(Booking booking);
        void onCancelClick(Booking booking); // Extended support
    }

    public BookingAdapter(Context context, List<Booking> bookings, OnBookingActionListener listener) {
        this.context = context;
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking b = bookings.get(position);

        holder.txtCategory.setText(b.getCategory() + " Service");
        holder.txtProvider.setText("by " + b.getProviderName());
        holder.txtPrice.setText(String.format(Locale.getDefault(), "₹%.2f", b.getTotalPrice()));
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

        // Dynamically style status pill based on status
        int bgDrawable;
        if (status.equals("COMPLETED")) {
            bgDrawable = R.drawable.bg_status_completed;
        } else if (status.equals("PENDING")) {
            bgDrawable = R.drawable.bg_status_pending;
        } else if (status.equals("CANCELLED") || status.equals("REJECTED")) {
            bgDrawable = R.drawable.bg_status_cancelled;
        } else {
            bgDrawable = R.drawable.bg_status_active; // Blue for ACCEPTED / IN_PROGRESS / ON_THE_WAY
        }
        holder.txtStatus.setBackground(ContextCompat.getDrawable(context, bgDrawable));

        // Show Rate & Review button if Completed and not yet rated
        if (status.equals("COMPLETED") && !b.isRated()) {
            holder.btnRate.setVisibility(View.VISIBLE);
            holder.btnRate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRateClick(b);
                }
            });
        } else {
            holder.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory, txtProvider, txtPrice, txtDate, txtSlot, txtStatus, txtPaymentMethod;
        Button btnRate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txt_booking_category);
            txtProvider = itemView.findViewById(R.id.txt_booking_provider);
            txtPrice = itemView.findViewById(R.id.txt_booking_price);
            txtDate = itemView.findViewById(R.id.txt_booking_date);
            txtSlot = itemView.findViewById(R.id.txt_booking_slot);
            txtStatus = itemView.findViewById(R.id.txt_booking_status);
            txtPaymentMethod = itemView.findViewById(R.id.txt_booking_payment_method);
            btnRate = itemView.findViewById(R.id.btn_rate_booking);
        }
    }
}
