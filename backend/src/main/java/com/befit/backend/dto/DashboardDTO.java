package com.befit.backend.dto;

import java.util.List;
import java.util.Map;

public class DashboardDTO {
    private int todayTotal;
    private int checkedIn;
    private int waiting;
    private int newInquiries;
    
    private List<Map<String, Object>> upcomingAppointments;
    private List<Map<String, Object>> currentRegistrations;

    // Getters and Setters
    public int getTodayTotal() { return todayTotal; }
    public void setTodayTotal(int todayTotal) { this.todayTotal = todayTotal; }
    public int getCheckedIn() { return checkedIn; }
    public void setCheckedIn(int checkedIn) { this.checkedIn = checkedIn; }
    public int getWaiting() { return waiting; }
    public void setWaiting(int waiting) { this.waiting = waiting; }
    public int getNewInquiries() { return newInquiries; }
    public void setNewInquiries(int newInquiries) { this.newInquiries = newInquiries; }
    public List<Map<String, Object>> getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(List<Map<String, Object>> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }
    public List<Map<String, Object>> getCurrentRegistrations() { return currentRegistrations; }
    public void setCurrentRegistrations(List<Map<String, Object>> currentRegistrations) { this.currentRegistrations = currentRegistrations; }
}