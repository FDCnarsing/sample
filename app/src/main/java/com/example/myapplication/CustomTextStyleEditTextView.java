package com.example.myapplication;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomTextStyleEditTextView extends AppCompatEditText {

    public interface onStyleChangedListener{
        void onStyleChangedUponBackspace(int prevStyleFlag, int newStyleFlag);
//        void onCheckStyleOnClick(int prevStyleFlag, int newStyleFlag);

    }
//    public interface onClickTextChangedListener{
//        void onCheckStyleOnClick(int prevStyleFlag, int newStyleFlag);
//    }

    public final static int STYLE_STRIKETHROUGH = 1;
    public final static int STYLE_BOLD = 2;
    public final static int STYLE_ITALIC = 4;
    public final static int STYLE_COLOR_BLUE = 8;
    public final static int STYLE_COLOR_BLACK = 16;
    public final static int STYLE_COLOR_RED = 32;


    private int mStyleFlag = 0;
    private int mPrevFlag = 0;
    private String mPrevTextHtml = "";
    private String mPrevText = "";
    private boolean mIsFlagChanged;
    private HashMap<Integer, Integer> mTextMap;
    private onStyleChangedListener mOnStyleChangedListener;
//    private onClickTextChangedListener mOnClickTextChangedListener;

    boolean isOnclicked = false;

    public CustomTextStyleEditTextView(Context context) {
        super(context);
        init();
    }

    public CustomTextStyleEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomTextStyleEditTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTextMap = new HashMap<>();
    }

    /**
     * Set text flag
     * @param flag
     */
    public void setTextStyle(int flag) {
        if (flag == mStyleFlag) {
            // if new style is equivalent to current style, return
            return;
        }

        mIsFlagChanged = true;

        mPrevFlag = mStyleFlag == 0 ? flag : mStyleFlag; // set previous flag
        mStyleFlag = flag; // set new flag
    }
    public void clicked (){
        applyTextStyle(getText(), 0, 0);
    }

    /**
     * Apply style to the text
     * @param text
     */
    private void applyTextStyle(CharSequence text, int lengthBefore, int lengthAfter) {
        try {
            boolean focussed = hasFocus();
            String addedText = "";
            boolean isBackSpaced = lengthBefore > lengthAfter;
            int positioon = getSelectionStart();
            String newText = "";

            if (text.length() == 0) {
                if (mOnStyleChangedListener != null) {
                    mOnStyleChangedListener.onStyleChangedUponBackspace(mPrevFlag, 0);
                }

                mPrevTextHtml = "";
                mPrevFlag = 0;
                mStyleFlag = 0;
                mPrevText = "";
                mTextMap.clear();

                requestFocus();

                return;
            }

            // clear focus to disable text watcher
            if (focussed) {
                clearFocus();
            }

            if (isBackSpaced) {
                if (mTextMap != null && mTextMap.size() > 0) {
                    // backspace is triggered
                    // check for change in style
                    // check text map for style mappings
                    int removedCharFlag = mTextMap.containsKey(mTextMap.size()-1) ? mTextMap.get(mTextMap.size()-1) : 0; // get the latest char flags on the list before removing
                    mTextMap.remove(mTextMap.size()-1); // remove latest char on textmap cause backspace is triggered

                    mIsFlagChanged = true;
                    int prevFlag = 0;
                    mPrevTextHtml = "";

                    // construct text html again
                    for(Map.Entry<Integer, Integer> entry : mTextMap.entrySet()) {
                        int key = entry.getKey();
                        int flags = entry.getValue();

                        mIsFlagChanged = prevFlag != flags;

                        newText += processStyleTextStyle(key == 0 ? flags : prevFlag , flags) + text.charAt(key);

                        prevFlag = flags;
                    }

                    mStyleFlag = prevFlag;
                    mPrevFlag = prevFlag;

                    if (mOnStyleChangedListener != null) {
                        mOnStyleChangedListener.onStyleChangedUponBackspace(removedCharFlag, prevFlag);
                    }
                }

            }
//            else if (isOnclicked){
//            else if (mTextMap.size() != 0 && mTextMap.size() > positioon && !mTextMap.get(mTextMap.size()-1).equals(mTextMap.get(positioon))){
//                    isOnclicked = true;
//
//                if (mTextMap != null && mTextMap.size() > 0) {
//                    // backspace is triggered
//                    // check for change in style
//                    // check text map for style mappings
//                    int removedCharFlag = mTextMap.containsKey(positioon) ? mTextMap.get(positioon) : 0; // get the latest char flags on the list before removing
////                    mTextMap.remove(mTextMap.size()-1); // remove latest char on textmap cause backspace is triggered
//
//                    mIsFlagChanged = true;
//                    int prevFlag = 0;
//                    mPrevTextHtml = "";
//
//                    // construct text html again
//                    for(Map.Entry<Integer, Integer> entry : mTextMap.entrySet()) {
//                        int key = entry.getKey();
//                        int flags = entry.getValue();
//
//                        mIsFlagChanged = prevFlag != flags;
//
//                        newText += processStyleTextStyle(key == 0 ? flags : prevFlag , flags) + text.charAt(key);
//
//                        prevFlag = flags;
//                    }
//
//                    mStyleFlag = prevFlag;
//                    mPrevFlag = prevFlag;
//
//                    if (mOnStyleChangedListener != null) {
//                        mOnStyleChangedListener.onCheckStyleOnClick(removedCharFlag, prevFlag);
//                    }
//                }
//            }


            else {

                if (!mPrevText.isEmpty()) {
                    addedText = text.subSequence(mPrevText.length(), text.length()) + "";
                } else {
                    addedText = text.toString();
                }

                if (addedText.length() > 1) {
                    // addedText is greater than 1, occurs when a paste happens
                    int pos = mPrevText.length();
                    for (int i = 0; i < addedText.length(); i++){
                        mTextMap.put(pos, mStyleFlag); // add to textmap
                        pos++;
                    }
                } else {
                    mTextMap.put(text.length() - 1, mStyleFlag); // add to textmap
                }

                newText = processStyleTextStyle() + "" + addedText;
            }

            setText(Html.fromHtml(mPrevTextHtml +"" +newText));

            // add cursor back to the end of edittext
//            if (!isOnclicked){
                setSelection(getText().length());
//            }
//            else
//            {
//                setSelection(positioon);
//            }

            // enable back focus
            if (focussed) {
                requestFocus();
            }

            mIsFlagChanged = false;
            mPrevTextHtml += newText;
            mPrevText = Jsoup.parse(mPrevTextHtml).text();

        } catch (Exception e) {
            requestFocus();
            e.printStackTrace();
        }

    }

    private String processStyleTextStyle() {
        return processStyleTextStyle(mPrevFlag, mStyleFlag);
    }

    /**
     * Iterate through flags and check the styles that is set then apply to text
     * @return String
     */
    private String processStyleTextStyle(int prevFlag, int styleFlag) {

        // flag array
        int[] flags = {STYLE_STRIKETHROUGH, STYLE_BOLD, STYLE_ITALIC,
                STYLE_COLOR_BLUE, STYLE_COLOR_BLACK, STYLE_COLOR_RED};

        String newText = "";

        if (prevFlag != styleFlag) {
            // there is a change in falgs
            // close styles that are not used
            for (int i = 0; i < flags.length; i++) {

                int flag = flags[i];

                if ((prevFlag & flag) == flag && (styleFlag & flag) != flag) {
                    // previous flag has particular style flag through but is removed on the current flag
                    newText += equivHtmlTag(flag, true);
                }
            }
        }

        // iterate through flag list
        for (int i = 0; i < flags.length; i++) {

            int flag = flags[i];

            if (mIsFlagChanged && (styleFlag & flag) == flag && (prevFlag & flag) == flag) {
                // previous flags has the particular style flag and current flags has no particular style flag
                newText += equivHtmlTag(flag, false);
            } else if ((styleFlag & flag) == flag && (prevFlag & flag) != flag){
                // previous flags has the particular style flag and current flags has no particular style flag
                newText += equivHtmlTag(flag, false);
            } else if ((styleFlag & flag) == flag && (prevFlag & flag) == flag) {
                // no changes on the text style
            }
        }

        return newText;
    }

    /**
     * Get equivalent HTML tag for different styles
     * @param styleFlag
     * @param isClose
     * @return String
     */
    private String equivHtmlTag(int styleFlag, boolean isClose) {
        switch (styleFlag) {
            case STYLE_BOLD:
                return !isClose ? "<b>" : "</b>";
            case STYLE_ITALIC:
                return !isClose ? "<i>" : "</i>";
            case STYLE_STRIKETHROUGH:
                return !isClose ? "<s>" : "</s>";
            case STYLE_COLOR_BLUE:
                return !isClose ? "<font color ='#0000ff'>" : "</font>";
            case STYLE_COLOR_BLACK:
                return !isClose ? "<font color='#000000'>" : "</font>";
            case STYLE_COLOR_RED:
                return !isClose ? "<font color='#ff0000'>" : "</font>";
            default:
                return "";
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (hasFocus()) {
            applyTextStyle(text, lengthBefore, lengthAfter);
            return;
        }

        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }
//
//    @Override
//    public void setOnClickListener(@Nullable OnClickListener l) {
//        if (callOnClick()) {
//            applyTextStyle(getText(), 0, 0);
//        }
//        super.setOnClickListener(l);
//    }

    public void setOnStyleChangedListener(onStyleChangedListener onStyleChangedListener) {
        mOnStyleChangedListener = onStyleChangedListener;
    }
//    public void onClickTextChangedListener(onClickTextChangedListener onClickTextChangedListener) {
//        mOnClickTextChangedListener = onClickTextChangedListener;
//    }
}

