package com.safra.safra.service;

import com.google.firebase.messaging.*;
import com.safra.safra.entity.Trip;
import com.safra.safra.entity.User;
import com.safra.safra.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final TripRepository tripRepository; // This will be injected by Lombok

    /**
     * Send booking confirmation notification
     */
    @Async
    public void sendBookingConfirmation(User passenger, Trip trip) {
        try {
            String token = passenger.getFcmToken();
            if (token == null || token.isEmpty()) {
                log.warn("No FCM token for user: {}", passenger.getId());
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", "BOOKING_CONFIRMED");
            data.put("tripId", trip.getId().toString());
            data.put("driverName", trip.getDriver().getName());
            data.put("startTime", trip.getStartTime().toString());

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("🎉 Booking Confirmed!")
                            .setBody(String.format("Your ride with %s is confirmed for %s",
                                    trip.getDriver().getName(),
                                    formatTime(trip.getStartTime())))
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setColor("#673AB7")
                                    .setIcon("ic_notification")
                                    .build())
                            .build())
                    .build();

            firebaseMessaging.send(message);
            log.info("Booking confirmation sent to user: {}", passenger.getId());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation: {}", e.getMessage());
        }
    }

    /**
     * Send trip cancellation notification
     */
    @Async
    public void sendTripCancellation(User user, Trip trip, boolean isDriver) {
        try {
            String token = user.getFcmToken();
            if (token == null || token.isEmpty()) return;

            Map<String, String> data = new HashMap<>();
            data.put("type", "TRIP_CANCELLED");
            data.put("tripId", trip.getId().toString());

            String title = isDriver ? "Trip Cancelled" : "Trip Cancelled by Driver";
            String body = isDriver
                    ? "You have cancelled your trip scheduled for " + formatTime(trip.getStartTime())
                    : String.format("Trip with %s has been cancelled", trip.getDriver().getName());

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("❌ " + title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setColor("#F44336")
                                    .build())
                            .build())
                    .build();

            firebaseMessaging.send(message);
        } catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    /**
     * Send trip reminder (1 hour before)
     * Runs every 5 minutes to check for upcoming trips
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void sendTripReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Find trips starting in approximately 1 hour
            List<Trip> upcomingTrips = tripRepository.findByStartTimeBetween(
                    now.plusMinutes(55),
                    now.plusMinutes(65)
            );

            log.info("Found {} upcoming trips for reminders", upcomingTrips.size());

            for (Trip trip : upcomingTrips) {
                // Only send reminders for trips that are OPEN or SCHEDULED
                if (!"OPEN".equals(trip.getStatus()) && !"SCHEDULED".equals(trip.getStatus())) {
                    continue;
                }

                // Send to driver
                sendTripReminder(trip.getDriver(), trip, true);

                // Send to all passengers
                for (User passenger : trip.getPassengers()) {
                    sendTripReminder(passenger, trip, false);
                }
            }
        } catch (Exception e) {
            log.error("Error sending trip reminders: {}", e.getMessage(), e);
        }
    }

    @Async
    private void sendTripReminder(User user, Trip trip, boolean isDriver) {
        try {
            String token = user.getFcmToken();
            if (token == null || token.isEmpty()) return;

            Map<String, String> data = new HashMap<>();
            data.put("type", "TRIP_REMINDER");
            data.put("tripId", trip.getId().toString());

            String title = isDriver ? "🚗 Your trip starts soon!" : "🚗 Trip Reminder";
            String body = String.format("Your ride %s in 1 hour at %s",
                    isDriver ? "starts" : "with " + trip.getDriver().getName() + " starts",
                    formatTime(trip.getStartTime()));

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setColor("#FF9800")
                                    .build())
                            .build())
                    .build();

            firebaseMessaging.send(message);
        } catch (Exception e) {
            log.error("Failed to send trip reminder: {}", e.getMessage());
        }
    }

    /**
     * Send driver arrival notification
     */
    @Async
    public void sendDriverArrivalNotification(User passenger, Trip trip, double distanceKm) {
        try {
            String token = passenger.getFcmToken();
            if (token == null || token.isEmpty()) return;

            Map<String, String> data = new HashMap<>();
            data.put("type", "DRIVER_ARRIVING");
            data.put("tripId", trip.getId().toString());
            data.put("distance", String.valueOf(distanceKm));

            String body = distanceKm < 0.5
                    ? String.format("%s has arrived at your location!", trip.getDriver().getName())
                    : String.format("%s is %.1f km away", trip.getDriver().getName(), distanceKm);

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("🚕 Driver Arriving")
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setColor("#4CAF50")
                                    .build())
                            .build())
                    .build();

            firebaseMessaging.send(message);
        } catch (Exception e) {
            log.error("Failed to send driver arrival notification: {}", e.getMessage());
        }
    }

    /**
     * Send rating request after trip completion
     */
    @Async
    public void sendRatingRequest(User passenger, Trip trip) {
        try {
            String token = passenger.getFcmToken();
            if (token == null || token.isEmpty()) return;

            Map<String, String> data = new HashMap<>();
            data.put("type", "RATE_TRIP");
            data.put("tripId", trip.getId().toString());

            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("⭐ Rate Your Trip")
                            .setBody(String.format("How was your ride with %s?",
                                    trip.getDriver().getName()))
                            .build())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.NORMAL)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setColor("#673AB7")
                                    .build())
                            .build())
                    .build();

            firebaseMessaging.send(message);
        } catch (Exception e) {
            log.error("Failed to send rating request: {}", e.getMessage());
        }
    }

    private String formatTime(LocalDateTime dateTime) {
        // Format as "MM/dd at h:mma"
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd 'at' h:mma"));
    }
}