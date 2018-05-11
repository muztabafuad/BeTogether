package fr.inria.yifan.mysensor.Support;

import java.util.Arrays;

public class SlideWindow {
    private int ct = 0;
    private float[] storage;

    SlideWindow(int size) {
        storage = new float[size];
    }

    public void putValue(float val) {
        // Initial fill
        if (ct == 0) {
            for (int i = 0; i < storage.length; i++) {
                storage[i] = val;
            }
        }
        storage[ct % storage.length] = val;
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
