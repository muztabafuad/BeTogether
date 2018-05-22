package fr.inria.yifan.mysensor.Sensing;

import java.util.Arrays;

public class SlideWindow {
    private float[] storage;
    private int ct;

    SlideWindow(int size, float initVal) {
        storage = new float[size];
        ct = 0;
        // Initial fill
        for (int i = 0; i < storage.length; i++) {
            storage[i] = initVal;
        }
        //System.out.println(Arrays.toString(storage));
    }

    public void putValue(float val) {
        // Initial update
        if (ct == 0) {
            for (int i = 0; i < storage.length; i++) {
                storage[i] = val;
            }
        }
        storage[ct % storage.length] = val;
        ct++;
        //System.out.println(Arrays.toString(storage));
    }

    public float getMean() {
        float sum = 0;
        for (float aStorage : storage) {
            sum += aStorage;
        }
        return sum / storage.length;
    }

    public float getLast() {
        return ct == 0 ? storage[ct] : storage[(ct -1) % storage.length];
    }

}
