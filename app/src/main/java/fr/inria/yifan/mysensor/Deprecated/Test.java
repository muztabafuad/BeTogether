package fr.inria.yifan.mysensor.Deprecated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        // Prints "Hello, World" to the terminal window.
        System.out.println("Hello, World");
        String s = "[Temperature, Pressure, Humidity, Temperature, Pressure, Humidity]";
        String replace = s.replace("[", "").replace("]", "");
        List<String> a = new ArrayList<>(Arrays.asList(replace.split(",")));
        System.out.println(a.get(0));
    }
}
