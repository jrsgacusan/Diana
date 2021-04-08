package com.example.occupines;

import android.view.View;
import android.widget.TextView;

import com.kizitonwose.calendarview.ui.ViewContainer;

public class MonthViewContainer extends ViewContainer {

    private final TextView textView;

    public MonthViewContainer(View view) {
        super(view);
        this.textView = view.findViewById(R.id.headerTextView);
    }

    public final TextView getTextView() {
        return this.textView;
    }
}
