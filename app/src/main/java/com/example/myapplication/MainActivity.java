package com.example.myapplication;

import static com.example.myapplication.CustomTextStyleEditTextView.STYLE_BOLD;
import static com.example.myapplication.CustomTextStyleEditTextView.STYLE_COLOR_BLACK;
import static com.example.myapplication.CustomTextStyleEditTextView.STYLE_COLOR_BLUE;
import static com.example.myapplication.CustomTextStyleEditTextView.STYLE_COLOR_RED;
import static com.example.myapplication.CustomTextStyleEditTextView.STYLE_ITALIC;
import static com.example.myapplication.CustomTextStyleEditTextView.STYLE_STRIKETHROUGH;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

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

        edittext.setOnStyleChangeListener(new CustomTextStyleEditTextView.OnStyleChangedListener() {
            @Override
            public void onStyleChange(List<Integer> styles) {
                mbold.setBackgroundColor(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.common_white));
                mbold.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.ic_bold));
                mbold.setCheckedState(false);
                mstrike.setCheckedState(false);
                mstrike.setBackgroundColor(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.common_white));
                mstrike.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.ic_strike));

                for (int i = 0; i < btn.length; i++) {
                    btn[i].setCheckedState(false);
                    btn[i] = MainActivity.this.findViewById(btn_id[i]);
                    btn[i].setImageDrawable(null);
                }

                for (Integer style : styles) {
                    switch (style) {
                        case STYLE_STRIKETHROUGH:
                            mstrike.setCheckedState(true);
                            mstrike.setBackgroundColor(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.common_darker_gray));
                            mstrike.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.ic_strike_selected));
                            break;
                        case STYLE_BOLD:
                            mbold.setCheckedState(true);
                            mbold.setBackgroundColor(ContextCompat.getColor(MainActivity.this.getApplicationContext(), R.color.common_darker_gray));
                            mbold.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.ic_bold_selected));
                            break;
                        case STYLE_COLOR_BLACK:
                            btn[0].setCheckedState(true);
                            MainActivity.this.setFocus(btn_unfocus, btn[0]);
                            break;
                        case STYLE_COLOR_BLUE:
                            btn[1].setCheckedState(true);
                            MainActivity.this.setFocus(btn_unfocus, btn[1]);
                            break;
                        case STYLE_COLOR_RED:
                            btn[2].setCheckedState(true);
                            MainActivity.this.setFocus(btn_unfocus, btn[2]);
                            break;
                    }
                }
                MainActivity.this.setTextStyle();
            }
        });

//        edittext.setTextStyle(new int[]{STYLE_BOLD, STYLE_STRIKETHROUGH, STYLE_COLOR_RED});

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
                if (button.getId() == R.id.chat_button_bold) {
                    button.switchCheckedState();
                    setTextStyle();
                } else if (button.getId() == R.id.chat_button_strike) {
                    button.switchCheckedState();
                    setTextStyle();
                } else if (button.getId() ==R.id.btn0) {
                    switchOffColorBtn();
                    button.switchCheckedState();
                    setTextStyle();
                    setFocus(btn_unfocus, btn[0]);}
                else if (button.getId() ==R.id.btn1){
                    switchOffColorBtn();
                    button.switchCheckedState();
                    setTextStyle();
                    setFocus(btn_unfocus, btn[1]);
                } else if (button.getId() == R.id.btn2){
                    switchOffColorBtn();
                    button.switchCheckedState();
                    setTextStyle();
                    setFocus(btn_unfocus, btn[2]);
                }
                if (button.getCheckedState()) {
                    if (button.getId() == R.id.chat_button_bold) {
                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_darker_gray));
                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_bold_selected));
                        setTextStyle();
                    }
                    if (button.getId() == R.id.chat_button_strike) {
                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_darker_gray));
                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_strike_selected));
                        setTextStyle();
                    }
                } else {
                    if (button.getId() == R.id.chat_button_bold) {
                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_white));
                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_bold));
                        setTextStyle();
                    }
                    if (button.getId() == R.id.chat_button_strike) {
                        button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.common_white));
                        button.setImageDrawable(getResources().getDrawable(R.drawable.ic_strike));
                        setTextStyle();
                    }
                }
            }
        }
    }

    private void switchOffColorBtn() {
        for (int i = 0; i < btn.length; i++) {
            btn[i].setCheckedState(false);
        }
    }

    private void setTextStyle() {
        List<Integer> flags = new ArrayList<>();
        if(mbold.getCheckedState()) flags.add(STYLE_BOLD);
        if(mstrike.getCheckedState()) flags.add(STYLE_STRIKETHROUGH);
        if(btn[0].getCheckedState()) flags.add(STYLE_COLOR_BLACK);
        if(btn[1].getCheckedState()) flags.add(STYLE_COLOR_BLUE);
        if(btn[2].getCheckedState()) flags.add(STYLE_COLOR_RED);

        edittext.setTextStyle(flags);
    }

    private void setFocus(WriteCustomButton btn_unfocus, WriteCustomButton btn_focus) {
        btn_unfocus.setImageDrawable(null);
        btn_focus.setImageDrawable(getDrawable(R.drawable.ic_check_normal));
        this.btn_unfocus = btn_focus;
    }
}
