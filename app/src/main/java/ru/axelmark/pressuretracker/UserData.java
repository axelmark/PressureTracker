package ru.axelmark.pressuretracker;

import android.content.Context;
import android.content.SharedPreferences;

public class UserData {
    public static final String PREFS_NAME = "UserDataPrefs";
    public static final String KEY_NAME = "name";
    public static final String KEY_AGE = "age";
    public static final String KEY_GENDER = "gender";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_WEIGHT = "weight";

    public String name;
    public int age;
    public String gender;
    public float height;
    public float weight;

    public UserData(String name, int age, String gender, float height, float weight) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
    }

    public static UserData fromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_NAME, "");
        int age = prefs.getInt(KEY_AGE, 0);
        String gender = prefs.getString(KEY_GENDER, "");
        float height = prefs.getFloat(KEY_HEIGHT, 0);
        float weight = prefs.getFloat(KEY_WEIGHT, 0);
        return new UserData(name, age, gender, height, weight);
    }

    public static void saveToPreferences(Context context, UserData userData) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_NAME, userData.name);
        editor.putInt(KEY_AGE, userData.age);
        editor.putString(KEY_GENDER, userData.gender);
        editor.putFloat(KEY_HEIGHT, userData.height);
        editor.putFloat(KEY_WEIGHT, userData.weight);
        editor.apply();
    }

    public float calculateBMI() {
        if (height <= 0 || weight <= 0) return 0;
        float heightInMeters = height / 100;
        return weight / (heightInMeters * heightInMeters);
    }
}