package fr.inria.yifan.mysensor.Support;

import java.util.Arrays;

public class SlideWindow {
    private int ct = 0;
    private float[] storage;

    SlideWindow(int size) {
        storage = new float[size];
    }

    public void putValue(float i) {
        storage[ct % storage.length] = i;
        ct++;
        System.out.println(Arrays.toString(storage));
    }

    public float getMean() {
        float sum = 0;
        for (float aStorage : storage) {
            sum += aStorage;
        }
        return sum / storage.length;
    }

}
