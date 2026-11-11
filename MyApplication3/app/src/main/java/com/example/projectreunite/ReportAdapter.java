package com.example.projectreunite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private final List<Report> reports;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public ReportAdapter(List<Report> reports) {
        this.reports = reports;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reports.get(position);

        holder.tvItemName.setText(report.itemName);
        holder.tvLocation.setText(report.location + (report.description.isEmpty() ? "" : " (" + report.description + ")"));
        holder.tvDate.setText(report.date);
        holder.tvFiler.setText("Filed by: " + report.filedBy);
        holder.tvStatus.setText(report.status);

        int colorRes = report.status.equals("LOST") ? R.color.status_lost : R.color.status_found;
        Context context = holder.itemView.getContext();
        int color = context.getResources().getColor(colorRes);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(8);
        holder.tvStatus.setBackground(gd);

        // Reset the image view for recycled items
        holder.ivPhoto.setImageBitmap(null);
        holder.ivPhoto.setVisibility(View.GONE);

        if (report.photoImage != null && !report.photoImage.isEmpty() && !report.photoImage.equals("null")) {
            holder.ivPhoto.setVisibility(View.VISIBLE);

            // Format URL cleanly to prevent double slashes
            String baseUrl = ApiClient.BASE_URL.endsWith("/") ? ApiClient.BASE_URL : ApiClient.BASE_URL + "/";
            String imageUrl = baseUrl + "uploads/" + report.photoImage;

            loadImage(holder.ivPhoto, imageUrl);
        }

        // Make the whole card clickable to go to details
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, ItemDetailsActivity.class);
            intent.putExtra("report_id", report.id);
            context.startActivity(intent);
        });
    }

    private void loadImage(ImageView imageView, String url) {
        imageView.setTag(url);
        executor.execute(() -> {
            try {
                InputStream is = new URL(url).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    imageView.post(() -> {
                        if (url.equals(imageView.getTag())) {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            } catch (Exception e) {
                // Fails silently if image is not found on the server
            }
        });
    }

    @Override
    public int getItemCount() { return reports.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvLocation, tvDate, tvFiler, tvItemName;
        ImageView ivPhoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvReportStatus);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvReportLocation);
            tvDate = itemView.findViewById(R.id.tvReportDate);
            tvFiler = itemView.findViewById(R.id.tvReportFiler);
            ivPhoto = itemView.findViewById(R.id.ivReportPhoto);
        }
    }
}