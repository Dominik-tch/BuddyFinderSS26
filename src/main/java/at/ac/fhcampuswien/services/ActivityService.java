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
    public void addActivity(Activity activity) {
        if (isInvalid(activity) || exists(activity)) {
            return;
        }
        activities.add(activity);
    }
    public void deleteActivity(UUID id) {
        activities.deleteById(id);
    }

    public boolean isInvalid(Activity activity) {
        return activity == null
                || activity.getTitle() == null
                || activity.getOwner() == null
                || activity.getPrice() < 0
                || activity.getLocation() == null
                || activity.getUserLimit() <= 0;
    }
    public boolean exists(Activity activity) {
        return activities.findAll().stream().anyMatch(a -> a.equals(activity));
    }

}
