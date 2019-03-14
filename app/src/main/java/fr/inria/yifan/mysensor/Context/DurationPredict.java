package fr.inria.yifan.mysensor.Context;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.time.LocalDateTime;

/**
 * This class learn and predict the duration of an user activity / physical environment.
 */

public class DurationPredict {


    @RequiresApi(api = Build.VERSION_CODES.O)
    public DurationPredict() {

        LocalDateTime currentTime = LocalDateTime.now();
        int day = currentTime.getDayOfWeek().getValue();
        int hour = currentTime.getHour();
        int minute = currentTime.getMinute();

        System.out.println(day + " " + hour + " " + minute);

    }

    public void updateDoorModel() {

    }

    public void updateGroundModel() {

    }

    public void updateActivityModel() {

    }

    public float predictDoorDuration() {
        return 0f;
    }

    public float predictGroundDuration() {
        return 0f;
    }

    public float predictActivityDuration() {
        return 0f;
    }

}
