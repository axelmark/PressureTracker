package ru.axelmark.pressuretracker;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Calendar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Версия приложения
        TextView versionTextView = findViewById(R.id.versionTextView);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionTextView.setText(getString(R.string.app_version_placeholder, version));
        } catch (PackageManager.NameNotFoundException e) {
            versionTextView.setText(R.string.app_version_placeholder);
        }

        // Копирайт с текущим годом
        TextView copyrightTextView = findViewById(R.id.copyrightTextView);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        copyrightTextView.setText(getString(R.string.about_copyright_placeholder, currentYear));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}