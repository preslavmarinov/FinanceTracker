package com.finance.tracker.dto;

public class ExportRequestDto {

    private String reportType = "monthly";
    private int month;
    private int year;

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
