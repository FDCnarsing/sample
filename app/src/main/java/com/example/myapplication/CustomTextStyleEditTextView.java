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
    private int[] mFlags = {STYLE_STRIKETHROUGH, STYLE_BOLD, STYLE_ITALIC,
            STYLE_COLOR_BLUE, STYLE_COLOR_BLACK, STYLE_COLOR_RED};
    private int[] oneStyleFlags = {STYLE_COLOR_BLUE, STYLE_COLOR_BLACK, STYLE_COLOR_RED}; // flags that can't be set to multipl
    private boolean mStyleFirstSet = true;
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

    public int getCurrentStyleFlag() {
        return mStyleFlag;
    }

    public boolean addStyleFlag(int flag) {
        if (hasMultipleStyleFlags(flag)) {
            // multiple flag detected can't add
            return false;
        }

        if (isNonMultipleStyleFlag(flag)) {
            // check if added flag is not for multiple style
            // e.g colors can be set to one color only, so remove previous set text color
            mPrevFlag = mStyleFlag; // set the current style flag to previous first
            clearNonMultipleStyleFlag();
        }

        setTextStyle(mStyleFlag | flag, true);
        return true;
    }

    public boolean removeStyleFlag(int flag) {
        if (hasMultipleStyleFlags(flag)) {
            // multiple flag detected can't add
            return false;
        }

        setTextStyle(mStyleFlag & (~ flag), false);
        return true;
    }

    /**
     * Set text flag
     * @param flag
     */
    public void setTextStyle(int flag, boolean isAdded) {
        if (flag == mStyleFlag) {
            // if new style is equivalent to current style, return
            return;
        }

        if (!isAdded) {
            mPrevFlag = mStyleFlag == 0 ? 0 : mStyleFlag; // set previous flag
        }

        mStyleFlag = flag; // set new flag
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

                    int prevFlag = 0;
                    mPrevTextHtml = "";

                    // construct text html again
                    for(Map.Entry<Integer, Integer> entry : mTextMap.entrySet()) {
                        int key = entry.getKey();
                        int flags = entry.getValue();

                        newText += processStyleTextStyle(prevFlag, flags) + text.charAt(key);

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
            setSelection(getText().length());

            // enable back focus
            if (focussed) {
                requestFocus();
            }

            mIsFlagChanged = false;
            mPrevTextHtml += newText;
            mPrevText = Jsoup.parse(mPrevTextHtml).text();
            mPrevFlag = mStyleFlag;

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

        boolean hasNonMultipleStyleFlg = false;

        String newText = "";

        if (prevFlag != styleFlag) {
            // there is a change in falgs
            // close styles that are not used
            for (int i = 0; i < mFlags.length; i++) {

                int flag = mFlags[i];

                if ((prevFlag & flag) == flag && (styleFlag & flag) != flag) {
                    // previous flag has particular style flag through but is removed on the current flag
                    newText += equivHtmlTag(flag, true);
                }

                if ((prevFlag & flag) == flag && isNonMultipleStyleFlag(flag)) {
                    hasNonMultipleStyleFlg = true;
                    newText += equivHtmlTag(flag, true);
                }
            }
        }

        // iterate through flag list
        for (int i = 0; i < mFlags.length; i++) {

            int flag = mFlags[i];

            if ((styleFlag & flag) == flag && (prevFlag & flag) == flag) {
                // no changes on current and previous styles
                // do nothing
                if (isNonMultipleStyleFlag(flag) && hasNonMultipleStyleFlg) {
                    newText += equivHtmlTag(flag, false);
                }
            } else if ((styleFlag & flag) == flag && (prevFlag & flag) != flag){
                // previous flags has the particular style flag and current flags has no particular style flag
                newText += equivHtmlTag(flag, false);
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

    public void setOnStyleChangedListener(onStyleChangedListener onStyleChangedListener) {
        mOnStyleChangedListener = onStyleChangedListener;
    }

    public boolean isNonMultipleStyleFlag(int flag) {
        return flag == STYLE_COLOR_BLUE || flag == STYLE_COLOR_RED || flag == STYLE_COLOR_BLACK;
    }

    public boolean hasMultipleStyleFlags(int flags) {
        int count=0;

        for (int i = 0; i < mFlags.length; i++) {
            int flag = mFlags[i];

            if ((flags & flag) == flag) {
                count++;
            }

            if (count > 1) {
                // flags consist of multiple flag
                return true;
            }
        }

        return false;
    }

    private void clearNonMultipleStyleFlag() {
        for (int i = 0; i < mFlags.length; i++) {
            int flag = mFlags[i];

            if (isNonMultipleStyleFlag(flag)) {
                // remove from current style
                mStyleFlag = (mStyleFlag & (~ flag));
            }
        }
    }


}

