package com.pesu.ooad.healthcare.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationFacade {

    // In a real application, these would connect to Twilio or an SMTP server.
    // For the scope of this OOAD project, we simulate the subsystems.

    private void sendEmail(Long userId, String message) {
        System.out.println("📧 [EMAIL SYSTEM] Sending to User ID " + userId + ": " + message);
    }

    private void sendSMS(Long userId, String message) {
        System.out.println("📱 [SMS SYSTEM] Sending to User ID " + userId + ": " + message);
    }

    // This is the Facade method that the rest of the system interacts with.
    public void notifyUser(Long userId, String message) {
        System.out.println("--- Triggering Notification Facade ---");
        sendEmail(userId, message);
        sendSMS(userId, message);
        System.out.println("--------------------------------------");
    }
}