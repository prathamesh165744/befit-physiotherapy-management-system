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

    // Getters
    public int getTodayTotal() { return todayTotal; }
    public int getCheckedIn() { return checkedIn; }
    public int getWaiting() { return waiting; }
    public int getNewInquiries() { return newInquiries; }
    public List<Map<String, Object>> getUpcomingAppointments() { return upcomingAppointments; }
    public List<Map<String, Object>> getCurrentRegistrations() { return currentRegistrations; }

    // Setters
    public void setTodayTotal(int todayTotal) { this.todayTotal = todayTotal; }
    public void setCheckedIn(int checkedIn) { this.checkedIn = checkedIn; }
    public void setWaiting(int waiting) { this.waiting = waiting; }
    public void setNewInquiries(int newInquiries) { this.newInquiries = newInquiries; }
    public void setUpcomingAppointments(List<Map<String, Object>> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }
    public void setCurrentRegistrations(List<Map<String, Object>> currentRegistrations) { this.currentRegistrations = currentRegistrations; }
}