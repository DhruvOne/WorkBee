package com.workbee.app.ui.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.Review;

import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> reviews;
    private final Context context;

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review r = reviews.get(position);

        holder.txtName.setText(r.getCustomerName());
        holder.txtComment.setText(r.getComment());
        holder.txtRating.setText(String.format(Locale.getDefault(), " %.1f", r.getRating()));

        // Format relative date
        if (r.getCreatedAt() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    r.getCreatedAt().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.txtDate.setText(timeAgo);
        } else {
            holder.txtDate.setText("Recently");
        }

        if (r.getCustomerImageUrl() != null && !r.getCustomerImageUrl().isEmpty()) {
            Glide.with(context)
                 .load(r.getCustomerImageUrl())
                 .placeholder(R.drawable.ic_workbee_logo)
                 .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_workbee_logo);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtDate, txtRating, txtComment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_customer);
            txtName = itemView.findViewById(R.id.txt_customer_name);
            txtDate = itemView.findViewById(R.id.txt_review_date);
            txtRating = itemView.findViewById(R.id.txt_rating);
            txtComment = itemView.findViewById(R.id.txt_comment);
        }
    }
}
