package com.example.projectreunite;

public class Report {
    public int id;
    public String reportId;
    public String itemName;
    public String description;
    public String location;
    public String date;
    public String photoImage;
    public String status;
    public String filedBy;

    public Report(int id, String reportId, String itemName, String description, String location, String date, String photoImage, String filedBy) {
        this.id = id;
        this.reportId = reportId;
        this.itemName = itemName;
        this.description = description;
        this.location = location;
        this.date = date;
        this.photoImage = photoImage;
        this.filedBy = filedBy;
        this.status = reportId != null && reportId.contains("FOUND") ? "FOUND" : "LOST";
    }
}
