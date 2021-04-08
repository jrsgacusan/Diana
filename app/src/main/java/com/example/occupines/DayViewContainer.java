package com.example.occupines;

import android.view.View;
import android.widget.TextView;

import com.example.occupines.fragments.FourthFragment;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.ViewContainer;

public class DayViewContainer extends ViewContainer {

    private final TextView textView;
    private CalendarDay day;

    public DayViewContainer(View view) {
        super(view);
        this.textView = view.findViewById(R.id.calendarDayText);

        view.setOnClickListener(v -> {
            if (day.getOwner() == DayOwner.THIS_MONTH) {
                FourthFragment.selectDate(day.getDate());
            }
        });
    }

    public TextView getTextView() {
        return textView;
    }

    public void setDay(CalendarDay day) {
        this.day = day;
    }
}
