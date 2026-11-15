package com.example.projectreunite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.HttpURLConnection;
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

        Context context = holder.itemView.getContext();
        boolean isLost = report.status.equalsIgnoreCase("LOST");
        int textColor = context.getResources().getColor(isLost ? R.color.status_lost : R.color.status_found);
        int bgColor = context.getResources().getColor(isLost ? R.color.status_lost_bg : R.color.status_found_bg);

        holder.tvStatus.setTextColor(textColor);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(16);
        holder.tvStatus.setBackground(gd);

        // Reset the image view for recycled items
        holder.ivPhoto.setImageBitmap(null);

        if (report.photoImage != null && !report.photoImage.isEmpty() && !report.photoImage.equalsIgnoreCase("null")) {
            holder.ivPhoto.setVisibility(View.VISIBLE);
            displayImage(holder.ivPhoto, report.photoImage);
        } else {
            // Display item placeholder image when report has no uploaded image in DB
            holder.ivPhoto.setVisibility(View.VISIBLE);
            holder.ivPhoto.setImageResource(R.drawable.img_item_placeholder);
        }

        // Make the whole card clickable to go to details
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, ItemDetailsActivity.class);
            intent.putExtra("report_id", report.id);
            context.startActivity(intent);
        });
    }

    private void displayImage(ImageView imageView, String photoData) {
        if (photoData == null || photoData.isEmpty() || photoData.equalsIgnoreCase("null")) {
            imageView.setImageResource(R.drawable.img_item_placeholder);
            imageView.setVisibility(View.VISIBLE);
            return;
        }

        imageView.setTag(photoData);

        String cleanData = photoData.trim();
        boolean isBase64 = false;
        if (cleanData.startsWith("data:image") || cleanData.contains("base64,")) {
            isBase64 = true;
        } else if (!cleanData.startsWith("http://") && !cleanData.startsWith("https://") && !cleanData.contains(".") && cleanData.length() > 50) {
            isBase64 = true;
        }

        if (isBase64) {
            executor.execute(() -> {
                try {
                    String base64Str = cleanData;
                    if (base64Str.contains("base64,")) {
                        base64Str = base64Str.substring(base64Str.indexOf("base64,") + 7);
                    }
                    byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        imageView.post(() -> {
                            if (photoData.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                                imageView.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        imageView.post(() -> {
                            imageView.setImageResource(R.drawable.img_item_placeholder);
                            imageView.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (Exception e) {
                    imageView.post(() -> {
                        imageView.setImageResource(R.drawable.img_item_placeholder);
                        imageView.setVisibility(View.VISIBLE);
                    });
                }
            });
        } else {
            String imageUrl;
            if (cleanData.startsWith("http://") || cleanData.startsWith("https://")) {
                imageUrl = cleanData;
            } else {
                String baseUrl = ApiClient.BASE_URL.endsWith("/") ? ApiClient.BASE_URL : ApiClient.BASE_URL + "/";
                if (cleanData.startsWith("uploads/")) {
                    imageUrl = baseUrl + cleanData;
                } else if (cleanData.startsWith("/uploads/")) {
                    imageUrl = baseUrl + cleanData.substring(1);
                } else if (cleanData.startsWith("/")) {
                    imageUrl = baseUrl + cleanData.substring(1);
                } else {
                    imageUrl = baseUrl + "uploads/" + cleanData;
                }
            }

            executor.execute(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap != null) {
                        imageView.post(() -> {
                            if (photoData.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                                imageView.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        imageView.post(() -> {
                            imageView.setImageResource(R.drawable.img_item_placeholder);
                            imageView.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (Exception e) {
                    imageView.post(() -> {
                        imageView.setImageResource(R.drawable.img_item_placeholder);
                        imageView.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
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