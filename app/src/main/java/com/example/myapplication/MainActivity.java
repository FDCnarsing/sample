package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class MainActivity extends AppCompatActivity {

    private WriteCustomButton mbold;
    private WriteCustomButton mstrike;
    private CustomTextStyleEditTextView edittext;
    private WriteCustomButton btn_unfocus;
    private WriteCustomButton[] btn = new WriteCustomButton[3];
    private int[] btn_id = {R.id.btn0, R.id.btn1, R.id.btn2};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mbold = findViewById(R.id.chat_button_bold);
        mstrike = findViewById(R.id.chat_button_strike);
        edittext = findViewById(R.id.chat_editText_message);
        edittext.setOnStyleChangedListener(new CustomTextStyleEditTextView.onStyleChangedListener() {
            @Override
            public void onStyleChangedUponBackspace(int prevStyleFlag, int newStyleFlag) {
                // clear button checks
                for (int i = 0; i < btn.length; i++) {
                    btn[i] = findViewById(btn_id[i]);
                    btn[i].setImageDrawable(null);
                }

                if ((newStyleFlag & CustomTextStyleEditTextView.STYLE_COLOR_BLUE) == CustomTextStyleEditTextView.STYLE_COLOR_BLUE) {
                    setFocus(btn_unfocus, btn[1]);
                }

                if ((newStyleFlag & CustomTextStyleEditTextView.STYLE_COLOR_RED) == CustomTextStyleEditTextView.STYLE_COLOR_RED) {
                    setFocus(btn_unfocus, btn[2]);
                }

                if ((newStyleFlag & CustomTextStyleEditTextView.STYLE_COLOR_BLACK) == CustomTextStyleEditTextView.STYLE_COLOR_BLACK) {
                    setFocus(btn_unfocus, btn[0]);
                }
            }
        });

        DecorationButtonListener decorationButtonListener = new DecorationButtonListener();
        mbold.setOnClickListener(decorationButtonListener);
        mstrike.setOnClickListener(decorationButtonListener);
        for (int i = 0; i < btn.length; i++) {
            btn[i] = findViewById(btn_id[i]);
            btn[i].setOnClickListener(decorationButtonListener);
        }
        btn_unfocus = btn[0];
        edittext.requestFocus();
    }
    class DecorationButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view instanceof WriteCustomButton) {
                WriteCustomButton button = (WriteCustomButton) view;
//                if (button.getId() == R.id.chat_button_bold) {
//                    button.switchCheckedState();
//                    edittext.setTextStyle(CustomTextStyleEditTextView.STYLE_BOLD);
//
//                } else if (button.getId() == R.id.chat_button_strike) {
//                    button.switchCheckedState();
//                    edittext.setTextStyle(CustomTextStyleEditTextView.STYLE_STRIKETHROUGH);
//                }
//                else
                if (button.getId() ==R.id.btn0) {
                    edittext.setTextStyle(CustomTextStyleEditTextView.STYLE_COLOR_BLACK);
                    setFocus(btn_unfocus, btn[0]);
                }
                else if (button.getId() ==R.id.btn1){
                    edittext.setTextStyle(CustomTextStyleEditTextView.STYLE_COLOR_BLUE);
                    setFocus(btn_unfocus, btn[1]);

                } else if (button.getId() == R.id.btn2){
                    edittext.setTextStyle(CustomTextStyleEditTextView.STYLE_COLOR_RED);
                    setFocus(btn_unfocus, btn[2]);
                }
//                if (button.getCheckedState()) {
//                    if (button.getId() == R.id.chat_button_bold) {
//                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_darker_gray));
////                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_bold_selected));
//                    }
//                    if (button.getId() == R.id.chat_button_strike) {
//                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_darker_gray));
////                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_strike_selected));
//                    }
//                }
//                else if (!button.getCheckedState() && firstClick) {
//                    if (button.getId() == R.id.chat_button_bold) {
//                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_white));
////                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_bold));
//                    }
//                    if (button.getId() == R.id.chat_button_strike) {
//                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_white));
////                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_strike));
//                    }
//                    firstClick = false;
//                }
//                else {
//                    if (button.getId() == R.id.chat_button_bold) {
//                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_white));
////                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_bold));
//                    }
//                    if (button.getId() == R.id.chat_button_strike) {
//                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_white));
////                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_strike));
//                    }
//                }
            }
        }
    }
    private void setFocus(WriteCustomButton btn_unfocus, WriteCustomButton btn_focus) {
        btn_unfocus.setImageDrawable(null);
        btn_focus.setImageDrawable(getDrawable(R.drawable.ic_check_normal));
        this.btn_unfocus = btn_focus;
    }
}