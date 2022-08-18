package com.example.myapplication;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Map;

public class CustomTextStyleEditTextView extends AppCompatEditText {

    public interface onStyleChangedListener{
        void onStyleChangedUponBackspace(int prevStyleFlag, int newStyleFlag);
    }

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

    /**
     * Apply style to the text
     * @param text
     */
    private void applyTextStyle(CharSequence text, int lengthBefore, int lengthAfter) {
        boolean focussed = hasFocus();
        String addedText = "";
        boolean isBackSpaced = lengthBefore > lengthAfter;
        String newText = "";

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

        } else {

            if (!mPrevText.isEmpty()) {
                String[] textSplit = text.toString().split(mPrevText);

                if (textSplit.length >= 2) {
                    addedText = textSplit[1];
                }
            } else {
                addedText = text.toString();
            }

            mTextMap.put(text.length() - 1, mStyleFlag); // add to textmap

            newText = processStyleTextStyle() + "" + addedText;
        }

        setText(Html.fromHtml(mPrevTextHtml +"" +newText));

        // add cursor back to the end of edittext
        setSelection(getText().length());

        // enable back focus
        if (focussed) {
            requestFocus();
        }

        mIsFlagChanged = false;
        mPrevTextHtml += newText;
        mPrevText = Jsoup.parse(mPrevTextHtml).text();
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
        int[] flags = {
//                STYLE_STRIKETHROUGH, STYLE_BOLD, STYLE_ITALIC,
                STYLE_COLOR_BLUE, STYLE_COLOR_BLACK, STYLE_COLOR_RED};

        String newText = "";

        // iterate through flag list
        for (int i = 0; i < flags.length; i++) {

            int flag = flags[i];

            if ((prevFlag & flag) == flag && (styleFlag & flag) != flag) {
                // previous flag has particular style flag through but is removed on the current flag
                newText += equivHtmlTag(flag, true);
            }

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
//            case STYLE_BOLD:
//                return !isClose ? "<b>" : "</b>";
//            case STYLE_ITALIC:
//                return !isClose ? "<i>" : "</i>";
//            case STYLE_STRIKETHROUGH:
//                return !isClose ? "<s>" : "</s>";
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
        if (hasFocus() && text.length() > 0) {
            applyTextStyle(text, lengthBefore, lengthAfter);
            return;
        }

        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    public void setOnStyleChangedListener(onStyleChangedListener onStyleChangedListener) {
        mOnStyleChangedListener = onStyleChangedListener;
    }
}
