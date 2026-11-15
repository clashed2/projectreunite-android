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
    public int userIdWhoFiled;
    public String category;

    public Report(int id, String reportId, String itemName, String description, String location, String date, String photoImage, String filedBy) {
        this(id, reportId, itemName, description, location, date, photoImage, filedBy, 0, "General");
    }

    public Report(int id, String reportId, String itemName, String description, String location, String date, String photoImage, String filedBy, int userIdWhoFiled, String category) {
        this.id = id;
        this.reportId = reportId;
        this.itemName = itemName;
        this.description = description;
        this.location = location;
        this.date = date;
        this.photoImage = photoImage;
        this.filedBy = filedBy;
        this.userIdWhoFiled = userIdWhoFiled;
        this.category = category != null && !category.isEmpty() ? category : "General";
        this.status = reportId != null && reportId.contains("FOUND") ? "FOUND" : "LOST";
    }
}