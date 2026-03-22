package ru.axelmark.pressuretracker;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportUtils {
    private final Activity activity;

    public ExportUtils(Activity activity) {
        this.activity = activity;
    }

    public void exportData(List<PressureMeasurement> measurements) {
        showExportDialog(measurements);
    }

    private void showExportDialog(List<PressureMeasurement> measurements) {
        String[] formats = {"CSV (Excel)", "PDF (Документ)"};

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(activity);

        builder.setTitle("Выберите формат экспорта")
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        exportToCSV(measurements);
                    } else if (which == 1) {
                        exportToPDF(measurements);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void exportToCSV(List<PressureMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            showToast("Нет данных для экспорта");
            return;
        }

        UserData userData = UserData.fromPreferences(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportCSVUsingMediaStore(measurements, userData);
        } else {
            exportCSVLegacy(measurements, userData);
        }
    }

    private void exportToPDF(List<PressureMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            showToast("Нет данных для экспорта");
            return;
        }

        UserData userData = UserData.fromPreferences(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportPDFUsingMediaStore(measurements, userData);
        } else {
            exportPDFLegacy(measurements, userData);
        }
    }

    private void exportCSVUsingMediaStore(List<PressureMeasurement> measurements, UserData userData) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "pressure_export_" + sdf.format(new Date()) + ".csv";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PressureTracker");

        Uri uri = activity.getContentResolver().insert(
                MediaStore.Files.getContentUri("external"),
                values
        );

        if (uri == null) {
            showToast("Экспорт не удался");
            return;
        }

        try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                showToast("Экспорт не удался");
                return;
            }

            writeCSVData(outputStream, measurements, userData);
            showToast("Данные экспортированы в CSV: " + fileName);

        } catch (Exception e) {
            showToast("Ошибка при экспорте CSV");
        }
    }

    private void exportCSVLegacy(List<PressureMeasurement> measurements, UserData userData) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "PressureTracker");

            if (!dir.exists() && !dir.mkdirs()) {
                showToast("Не удалось создать директорию");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "pressure_export_" + sdf.format(new Date()) + ".csv";
            File file = new File(dir, fileName);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                writeCSVData(outputStream, measurements, userData);

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(file));
                activity.sendBroadcast(mediaScanIntent);

                showToast("Данные экспортированы в CSV: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showToast("Ошибка при экспорте CSV");
        }
    }

    private void exportPDFUsingMediaStore(List<PressureMeasurement> measurements, UserData userData) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "pressure_export_" + sdf.format(new Date()) + ".pdf";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PressureTracker");

        Uri uri = activity.getContentResolver().insert(
                MediaStore.Files.getContentUri("external"),
                values
        );

        if (uri == null) {
            showToast("Экспорт не удался");
            return;
        }

        try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                showToast("Экспорт не удался");
                return;
            }

            createPdfDocument(outputStream, measurements, userData);
            showToast("Данные экспортированы в PDF: " + fileName);

        } catch (Exception e) {
            showToast("Ошибка при экспорте PDF");
        }
    }

    private void exportPDFLegacy(List<PressureMeasurement> measurements, UserData userData) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "PressureTracker");

            if (!dir.exists() && !dir.mkdirs()) {
                showToast("Не удалось создать директорию");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "pressure_export_" + sdf.format(new Date()) + ".pdf";
            File file = new File(dir, fileName);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                createPdfDocument(outputStream, measurements, userData);

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(file));
                activity.sendBroadcast(mediaScanIntent);

                showToast("Данные экспортированы в PDF: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showToast("Ошибка при экспорте PDF");
        }
    }

    private void writeCSVData(OutputStream outputStream,
                              List<PressureMeasurement> measurements,
                              UserData userData) throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        Context context = activity.getApplicationContext();

        writer.append("Отчет о измерениях артериального давления\n\n");
        writer.append("Данные пользователя:\n");
        if (!userData.name.isEmpty()) {
            writer.append("Имя: ").append(userData.name).append("\n");
        }
        if (userData.age > 0) {
            writer.append("Возраст: ").append(String.valueOf(userData.age)).append("\n");
        }
        if (!userData.gender.isEmpty()) {
            writer.append("Пол: ").append(userData.gender.equals("male") ? "Мужской" : "Женский").append("\n");
        }
        if (userData.height > 0) {
            writer.append("Рост: ").append(String.format(Locale.getDefault(), "%.1f", userData.height)).append(" см\n");
        }
        if (userData.weight > 0) {
            writer.append("Вес: ").append(String.format(Locale.getDefault(), "%.1f", userData.weight)).append(" кг\n");
        }
        if (userData.height > 0 && userData.weight > 0) {
            float bmi = userData.calculateBMI();
            writer.append("ИМТ: ").append(String.format(Locale.getDefault(), "%.1f", bmi)).append(" (")
                    .append(getBMICategory(bmi, context)).append(")\n");
        }
        writer.append("\n");

        writer.append("История измерений:\n");
        writer.append("Дата,Систолическое,Диастолическое,Пульс,Категория,Принято лекарство,Примечание\n");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        for (PressureMeasurement m : measurements) {
            writer.append(dateFormat.format(new Date(m.timestamp)))
                    .append(",")
                    .append(String.valueOf(m.systolic))
                    .append(",")
                    .append(String.valueOf(m.diastolic))
                    .append(",")
                    .append(String.valueOf(m.pulse))
                    .append(",")
                    .append(getPressureCategory(m.getPressureCategory(), context))
                    .append(",")
                    .append(m.medicationTaken ? "Да" : "Нет")
                    .append(",")
                    .append(m.note == null ? "" : m.note.replace(",", ";"))
                    .append("\n");
        }

        writer.flush();
    }

    private void createPdfDocument(OutputStream outputStream,
                                   List<PressureMeasurement> measurements,
                                   UserData userData) throws Exception {
        int pageWidth = 595;
        int pageHeight = 842;
        Context context = activity.getApplicationContext();

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        float margin = 50;
        float y = margin;

        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Отчет о измерениях артериального давления", margin, y, paint);
        y += 30;

        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Данные пользователя:", margin, y, paint);
        y += 20;

        if (!userData.name.isEmpty()) {
            canvas.drawText("Имя: " + userData.name, margin, y, paint);
            y += 20;
        }
        if (userData.age > 0) {
            canvas.drawText("Возраст: " + userData.age, margin, y, paint);
            y += 20;
        }
        if (!userData.gender.isEmpty()) {
            canvas.drawText("Пол: " + (userData.gender.equals("male") ? "Мужской" : "Женский"), margin, y, paint);
            y += 20;
        }
        if (userData.height > 0) {
            canvas.drawText("Рост: " + String.format(Locale.getDefault(), "%.1f", userData.height) + " см", margin, y, paint);
            y += 20;
        }
        if (userData.weight > 0) {
            canvas.drawText("Вес: " + String.format(Locale.getDefault(), "%.1f", userData.weight) + " кг", margin, y, paint);
            y += 20;
        }
        if (userData.height > 0 && userData.weight > 0) {
            float bmi = userData.calculateBMI();
            canvas.drawText("ИМТ: " + String.format(Locale.getDefault(), "%.1f", bmi) +
                    " (" + getBMICategory(bmi, context) + ")", margin, y, paint);
            y += 30;
        }

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("История измерений:", margin, y, paint);
        y += 25;

        String[] headers = {"Дата", "Систолическое", "Диастолическое", "Пульс", "Категория", "Принято лекарство", "Примечание"};
        float[] columnWidths = {120, 80, 80, 60, 80, 80, 150};

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        float x = margin;
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], x, y, paint);
            x += columnWidths[i];
        }
        y += 25;

        paint.setStrokeWidth(1);
        canvas.drawLine(margin, y, pageWidth - margin, y, paint);
        y += 15;

        paint.setTypeface(Typeface.DEFAULT);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        for (PressureMeasurement m : measurements) {
            if (y > pageHeight - margin - 20) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;

                paint.setTypeface(Typeface.DEFAULT_BOLD);
                x = margin;
                for (int i = 0; i < headers.length; i++) {
                    canvas.drawText(headers[i], x, y, paint);
                    x += columnWidths[i];
                }
                y += 25;
                canvas.drawLine(margin, y, pageWidth - margin, y, paint);
                y += 15;
                paint.setTypeface(Typeface.DEFAULT);
            }

            x = margin;
            canvas.drawText(dateFormat.format(new Date(m.timestamp)), x, y, paint);
            x += columnWidths[0];

            canvas.drawText(String.valueOf(m.systolic), x, y, paint);
            x += columnWidths[1];

            canvas.drawText(String.valueOf(m.diastolic), x, y, paint);
            x += columnWidths[2];

            canvas.drawText(String.valueOf(m.pulse), x, y, paint);
            x += columnWidths[3];

            String pressureCategory = getPressureCategory(m.getPressureCategory(), context);
            canvas.drawText(pressureCategory, x, y, paint);
            x += columnWidths[4];

            canvas.drawText(m.medicationTaken ? "Да" : "Нет", x, y, paint);
            x += columnWidths[5];

            String note = m.note == null ? "" : m.note;
            if (note.length() > 20) {
                note = note.substring(0, 17) + "...";
            }
            canvas.drawText(note, x, y, paint);

            y += 20;
        }

        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    private String getBMICategory(float bmi, Context context) {
        if (bmi < 18.5) return context.getString(R.string.bmi_underweight);
        if (bmi < 25) return context.getString(R.string.bmi_normal);
        if (bmi < 30) return context.getString(R.string.bmi_overweight);
        return context.getString(R.string.bmi_obese);
    }

    private String getPressureCategory(String category, Context context) {
        switch (category) {
            case "low": return context.getString(R.string.pressure_low);
            case "normal": return context.getString(R.string.pressure_normal);
            case "elevated": return context.getString(R.string.pressure_elevated);
            case "high": return context.getString(R.string.pressure_high);
            default: return "";
        }
    }
}