package com.erickoeckel.tasktimer;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class StickyAutoCompleteTextView extends MaterialAutoCompleteTextView {

    private boolean keepOpen = true;

    public StickyAutoCompleteTextView(Context c) { super(c); init(); }
    public StickyAutoCompleteTextView(Context c, AttributeSet a) { super(c, a); init(); }
    public StickyAutoCompleteTextView(Context c, AttributeSet a, int defStyleAttr) { super(c, a, defStyleAttr); init(); }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setCursorVisible(false);
    }

    @Override
    public void dismissDropDown() {
        if (!keepOpen) super.dismissDropDown();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        keepOpen = focused;
        if (!focused) {
            super.dismissDropDown();
        }
    }
}
