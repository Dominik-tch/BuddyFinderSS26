package at.ac.fhcampuswien.services;

import at.ac.fhcampuswien.exceptions.ActivityNotFoundException;
import at.ac.fhcampuswien.models.Activity;
import at.ac.fhcampuswien.repositories.ActivityRepository;

import java.util.List;
import java.util.UUID;

public class ActivityService {
    private ActivityRepository activities;

    public ActivityService(ActivityRepository activities) {
        this.activities = activities;
    }

    public List<Activity> getAllActivities() {
        List<Activity> allActivities = activities.findAll();
        if (allActivities.isEmpty()) {
            throw new ActivityNotFoundException("No activities in the database");
        }
        return allActivities;
    }
    public List<Activity> getAllOwnedActivities(String owner) {
        List<Activity> allActivities = activities.findAllOwned(owner);
        if (allActivities.isEmpty()) {
            throw new ActivityNotFoundException("No owned activities in the database");
        }
        return allActivities;
    }
    public List<Activity> getAllJoinedActivities(String userId) {
        List<Activity> allActivities = activities.findAllJoined(userId);
        if (allActivities.isEmpty()) {
            throw new ActivityNotFoundException("No joined activities in the database");
        }
        return allActivities;
    }

    public List<Activity> searchActivities(String title, String location, Integer maxPrice) {
        return activities.search(title, location, maxPrice);
    }

    public void joinActivity(UUID userId, UUID activityId) {

        if (activities.isUserJoined(userId, activityId)) {
            throw new IllegalStateException("User already joined this activity!");
        }

        int currentParticipants = activities.countParticipants(activityId);

        Activity activity = activities.getActivityById(activityId);

        if (currentParticipants >= activity.getUserLimit()) {
            throw new IllegalStateException("Activity is already full!");
        }

        activities.joinActivity(userId, activityId);
    }

    public void addActivity(Activity activity) {
        activities.add(activity);
    }
    public void deleteActivity(UUID id) {
        activities.deleteById(id);
    }
    public void updateActivity(UUID id, Activity updatedActivity) {
        activities.update(id, updatedActivity);
    }
    public void leaveActivity(UUID userId, UUID activityId) {
        activities.leaveActivity(userId, activityId);
    }

    public boolean isInvalid(Activity activity) {
        return activity == null
                || activity.getTitle() == null
                || activity.getPrice() < 0
                || activity.getLocation() == null
                || activity.getUserLimit() <= 0;
    }
    public boolean exists(Activity activity) {
        return activities.findAll().stream().anyMatch(a -> a.equals(activity));
    }

    public Activity getActivityById(UUID id) {
        Activity activity = activities.getActivityById(id);
        if (activity == null) {
            throw new ActivityNotFoundException("Activity not found.");
        }
        return activity;
    }

    public void updateWeather(UUID id, String weather) {
        activities.updateWeather(id, weather);
    }
}
