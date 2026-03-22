package ru.axelmark.pressuretracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PressureAdapter extends RecyclerView.Adapter<PressureAdapter.ViewHolder> {
    private List<PressureMeasurement> measurements;
    private Set<Integer> expandedPositions = new HashSet<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView pressureValue;
        public TextView pulseValue;
        public TextView dateValue;
        public TextView statusLabel;
        public TextView noteValue;
        public TextView noteLabel;
        public ImageView expandIcon;
        public LinearLayout noteHeader;
        public TextView medicationIndicator;

        public ViewHolder(View view) {
            super(view);
            pressureValue = view.findViewById(R.id.pressureValue);
            pulseValue = view.findViewById(R.id.pulseValue);
            dateValue = view.findViewById(R.id.dateValue);
            statusLabel = view.findViewById(R.id.statusLabel);
            noteValue = view.findViewById(R.id.noteValue);
            noteLabel = view.findViewById(R.id.noteLabel);
            expandIcon = view.findViewById(R.id.expandIcon);
            noteHeader = view.findViewById(R.id.noteHeader);
            medicationIndicator = view.findViewById(R.id.medicationIndicator);
        }
    }

    public PressureAdapter(List<PressureMeasurement> measurements) {
        this.measurements = measurements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_pressure, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PressureMeasurement item = measurements.get(position);
        holder.pressureValue.setText(item.systolic + "/" + item.diastolic);
        holder.pulseValue.setText(String.valueOf(item.pulse));

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        holder.dateValue.setText(sdf.format(item.timestamp));

        int colorRes;
        String statusText;
        switch (item.getPressureCategory()) {
            case "low":
                colorRes = android.R.color.holo_blue_light;
                statusText = "Пониженное";
                break;
            case "normal":
                colorRes = android.R.color.holo_green_light;
                statusText = "Норма";
                break;
            case "elevated":
                colorRes = android.R.color.holo_orange_light;
                statusText = "Повышенное";
                break;
            case "high":
                colorRes = android.R.color.holo_red_light;
                statusText = "Высокое";
                break;
            default:
                colorRes = android.R.color.black;
                statusText = "";
        }

        holder.pressureValue.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));
        holder.statusLabel.setText(statusText);

        // Индикатор приёма лекарства
        if (item.medicationTaken) {
            holder.medicationIndicator.setText("💊");
            holder.medicationIndicator.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
        } else {
            holder.medicationIndicator.setText("●");
            holder.medicationIndicator.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.medication_not_taken_color));
        }

        boolean hasNote = item.note != null && !item.note.trim().isEmpty();
        if (hasNote) {
            holder.noteHeader.setVisibility(View.VISIBLE);
            holder.noteValue.setText(item.note);

            boolean isExpanded = expandedPositions.contains(position);
            holder.noteValue.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.expandIcon.setImageResource(isExpanded ?
                    R.drawable.ic_expand_less : R.drawable.ic_expand_more);

            holder.itemView.setOnClickListener(v -> {
                if (expandedPositions.contains(position)) {
                    expandedPositions.remove(position);
                } else {
                    expandedPositions.add(position);
                }
                notifyItemChanged(position);
            });
        } else {
            holder.noteHeader.setVisibility(View.GONE);
            holder.noteValue.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return measurements.size();
    }

    public void updateData(List<PressureMeasurement> newMeasurements) {
        measurements = newMeasurements;
        expandedPositions.clear();
        notifyDataSetChanged();
    }

    public PressureMeasurement getItem(int position) {
        return measurements.get(position);
    }
}