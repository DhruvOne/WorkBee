package com.workbee.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;

import java.util.List;
import java.util.Locale;

public class ProviderJobRequestAdapter extends RecyclerView.Adapter<ProviderJobRequestAdapter.ViewHolder> {

    private final Context context;
    private final List<Booking> requests;
    private final OnRequestListener listener;

    public interface OnRequestListener {
        void onAccept(Booking booking);
        void onReject(Booking booking);
    }

    public ProviderJobRequestAdapter(Context context, List<Booking> requests, OnRequestListener listener) {
        this.context = context;
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_provider_job_request, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking b = requests.get(position);

        holder.txtCustomer.setText(b.getCustomerName());
        holder.txtPrice.setText(String.format(Locale.getDefault(), "₹%.2f", b.getTotalPrice()));
        holder.txtDesc.setText(b.getDescription());
        holder.txtAddress.setText(b.getAddress());
        holder.txtDate.setText("Date: " + b.getDate());
        holder.txtSlot.setText("Slot: " + b.getTimeSlot());

        String payMethod = b.getPaymentMethod();
        if (payMethod == null || payMethod.equalsIgnoreCase("CASH")) {
            holder.txtPaymentMethod.setText("Payment: 💵 Cash on Service");
        } else {
            holder.txtPaymentMethod.setText("Payment: 💳 Paid Online");
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(b);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(b);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCustomer, txtPrice, txtDesc, txtAddress, txtDate, txtSlot, txtPaymentMethod;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCustomer = itemView.findViewById(R.id.txt_req_customer);
            txtPrice = itemView.findViewById(R.id.txt_req_price);
            txtDesc = itemView.findViewById(R.id.txt_req_desc);
            txtAddress = itemView.findViewById(R.id.txt_req_address);
            txtDate = itemView.findViewById(R.id.txt_req_date);
            txtSlot = itemView.findViewById(R.id.txt_req_slot);
            txtPaymentMethod = itemView.findViewById(R.id.txt_req_payment_method);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
