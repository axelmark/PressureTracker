package ru.axelmark.pressuretracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class UserDataActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText ageInput;
    private EditText heightInput;
    private EditText weightInput;
    private RadioGroup genderGroup;
    private RadioButton maleRadio;
    private RadioButton femaleRadio;
    private TextView bmiResult;
    private TextView bmiCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_data);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        nameInput = findViewById(R.id.nameInput);
        ageInput = findViewById(R.id.ageInput);
        heightInput = findViewById(R.id.heightInput);
        weightInput = findViewById(R.id.weightInput);
        genderGroup = findViewById(R.id.genderGroup);
        maleRadio = findViewById(R.id.maleRadio);
        femaleRadio = findViewById(R.id.femaleRadio);
        bmiResult = findViewById(R.id.bmiResult);
        bmiCategory = findViewById(R.id.bmiCategory);
        Button saveButton = findViewById(R.id.saveButton);
//        Button calculateBmiButton = findViewById(R.id.calculateBmiButton);

        loadUserData();

        heightInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (heightInput.getText().length() > 0 && weightInput.getText().length() > 0) {
                    calculateBMI();
                }
            }
        });

        weightInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (heightInput.getText().length() > 0 && weightInput.getText().length() > 0) {
                    calculateBMI();
                }
            }
        });

//        calculateBmiButton.setOnClickListener(v -> calculateBMI());
        saveButton.setOnClickListener(v -> saveUserData());
    }

    private void calculateBMI() {
        try {
            float height = Float.parseFloat(heightInput.getText().toString());
            float weight = Float.parseFloat(weightInput.getText().toString());

            if (height <= 0 || weight <= 0) {
                bmiResult.setText("Введите корректные данные");
                bmiCategory.setText("");
                return;
            }

            float heightInMeters = height / 100;
            float bmi = weight / (heightInMeters * heightInMeters);

            bmiResult.setText(String.format("ИМТ: %.1f", bmi));

            String category;
            int color;
            if (bmi < 18.5) {
                category = "Недостаточный вес";
                color = R.color.bmi_underweight;
            } else if (bmi < 25) {
                category = "Нормальный вес";
                color = R.color.bmi_normal;
            } else if (bmi < 30) {
                category = "Избыточный вес";
                color = R.color.bmi_overweight;
            } else {
                category = "Ожирение";
                color = R.color.bmi_obese;
            }

            bmiCategory.setText(category);
            bmiCategory.setTextColor(ContextCompat.getColor(this, color));

        } catch (NumberFormatException e) {
            bmiResult.setText("Введите корректные данные");
            bmiCategory.setText("");
        }
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(UserData.PREFS_NAME, MODE_PRIVATE);
        nameInput.setText(prefs.getString(UserData.KEY_NAME, ""));

        int age = prefs.getInt(UserData.KEY_AGE, 0);
        if (age > 0) {
            ageInput.setText(String.valueOf(age));
        }

        float height = prefs.getFloat(UserData.KEY_HEIGHT, 0);
        if (height > 0) {
            heightInput.setText(String.valueOf(height));
        }

        float weight = prefs.getFloat(UserData.KEY_WEIGHT, 0);
        if (weight > 0) {
            weightInput.setText(String.valueOf(weight));
        }

        String gender = prefs.getString(UserData.KEY_GENDER, "");
        if ("male".equals(gender)) {
            maleRadio.setChecked(true);
        } else if ("female".equals(gender)) {
            femaleRadio.setChecked(true);
        }

        if (height > 0 && weight > 0) {
            calculateBMI();
        }
    }

    private void saveUserData() {
        String name = nameInput.getText().toString().trim();
        String ageStr = ageInput.getText().toString().trim();
        String heightStr = heightInput.getText().toString().trim();
        String weightStr = weightInput.getText().toString().trim();

        int age = 0;
        float height = 0;
        float weight = 0;

        try {
            if (!ageStr.isEmpty()) {
                age = Integer.parseInt(ageStr);
                if (age < 1 || age > 120) {
                    Toast.makeText(this, "Введите корректный возраст (1-120)", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите число для возраста", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!heightStr.isEmpty()) {
                height = Float.parseFloat(heightStr);
                if (height < 50 || height > 250) {
                    Toast.makeText(this, "Введите корректный рост (50-250 см)", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите число для роста", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!weightStr.isEmpty()) {
                weight = Float.parseFloat(weightStr);
                if (weight < 5 || weight > 300) {
                    Toast.makeText(this, "Введите корректный вес (5-300 кг)", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите число для веса", Toast.LENGTH_SHORT).show();
            return;
        }

        String gender = "";
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.maleRadio) {
            gender = "male";
        } else if (selectedId == R.id.femaleRadio) {
            gender = "female";
        }

        UserData userData = new UserData(name, age, gender, height, weight);
        UserData.saveToPreferences(this, userData);

        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }
}