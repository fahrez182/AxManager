package com.frb.axmanager;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;

import androidx.annotation.Nullable;

public class Toast extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String text = getIntent().getStringExtra("text");
        if (text == null || text.trim().isEmpty()) text = "Toasted!";

        android.widget.Toast toast = android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();

        finish();
    }
}
