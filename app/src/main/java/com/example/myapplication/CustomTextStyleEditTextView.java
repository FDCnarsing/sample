package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private int[] mFlags = {STYLE_COLOR_RED, STYLE_COLOR_BLACK, STYLE_COLOR_BLUE, STYLE_STRIKETHROUGH, STYLE_ITALIC,STYLE_BOLD};
    private int[] oneStyleFlags = {STYLE_COLOR_BLUE, STYLE_COLOR_BLACK, STYLE_COLOR_RED}; // flags that can't be set to multiple
    private boolean mStyleFirstSet = true;
    private boolean mIsTextProcessing = false;

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

    // for one flag only
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

    // for one flag only
    public boolean removeStyleFlag(int flag) {
        if (hasMultipleStyleFlags(flag)) {
            // multiple flag detected can't add
            return false;
        }

        setTextStyle(mStyleFlag & (~ flag), false);
        return true;
    }

    /**
     * Set text multiple flags
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
            String addedText = "";
            boolean isBackSpaced = lengthBefore > lengthAfter;
            String newText = "";
            String htmlText = Html.toHtml(getText());

            mIsTextProcessing = true;

            if (text.length() == 0) {
                if (mOnStyleChangedListener != null) {
                    mOnStyleChangedListener.onStyleChangedUponBackspace(mPrevFlag, 0);
                }

                mPrevTextHtml = "";
                mPrevFlag = 0;
                mStyleFlag = 0;
                mPrevText = "";

                if (mTextMap != null) {
                    mTextMap.clear();
                }

                mIsTextProcessing = false;

                return;
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

                        newText += processStyleTextStyle(String.valueOf(text.charAt(key)), prevFlag, flags);
                        newText = alterTags(newText);

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

                if (addedText.length() == 1) {
                    newText =  processStyleTextStyle() + alterTags(addedText);
                    mTextMap.put(text.length() - 1, mStyleFlag);
                } else {
                    // addedText is greater than 1, occurs when a paste happens
                    mTextMap.clear(); // clear textmap
                    boolean isOpenHtml = false;
                    String tag = "";
                    mPrevTextHtml = "";
                    mPrevFlag = 0;
                    mStyleFlag = 0;
                    int lastOpenFlag = 0;
                    int textPos = 0;
                    boolean isAdd = false;
                    int currFlag = 0;
                    boolean isHtml = false;

                    htmlText = alterTags(htmlText);

                    for (int i = 0; i < htmlText.length(); i++) {
                        String charaString = String.valueOf(htmlText.charAt(i));
                        if (charaString.equals("<")) {
                            isOpenHtml = true;
                            tag = ""; // reset tag
                            tag += charaString;
                            continue;
                        }

                        if (charaString.equals(">")) {
                            isOpenHtml = false;
                            tag += charaString;
                        }

                        if (isOpenHtml) {
                            tag += charaString;
                            continue;
                        }

                        if (!tag.isEmpty()) {
                            // loop if tag is not empty
                            for (int x = 0; x < mFlags.length; x++) {

                                int flag = mFlags[x];

                                if (tag.equals(equivHtmlTag(flag, false))) {
                                    isAdd = !charaString.contains(">");
                                    addStyleFlag(flag);
                                    lastOpenFlag = flag;
                                    isHtml = true;
                                } else if (tag.equals(equivHtmlTag(flag, true)) && lastOpenFlag == flag) {
                                    removeStyleFlag(flag);
                                    isAdd = !charaString.contains(">");
                                    isHtml = true;
                                }
                            }
                        } else {
                            isAdd = !charaString.contains(">");
                        }

                        if (!isAdd) {
                            continue;
                        }

                        if (!tag.contains("</")) {
                            mTextMap.put(textPos, mStyleFlag);
                            textPos++;
                            tag="";
                        }
                    }

                    if (!isHtml) {
                        // text doesn't contain any style
                        int pos = mPrevText.length();
                        for (int i = 0; i < addedText.length(); i++) {
                            mTextMap.put(pos, mStyleFlag); // add to textmap
                            pos++;
                        }
                    }

                    newText = htmlText;
                }
            }

            addText(newText, true);

            mIsFlagChanged = false;
            mPrevTextHtml += newText;
            mPrevText = Html.fromHtml(mPrevTextHtml).toString();
            mPrevFlag = mStyleFlag;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mIsTextProcessing = false;
        }

    }

    private void addText(String text, boolean isHtml) {

        getText().clear();

        if (isHtml) {
            append(Html.fromHtml(mPrevTextHtml +""+text));
            return;
        }

        append(String.format("%1$s%2$s",mPrevText, text));
    }

    private String processStyleTextStyle() {
        return processStyleTextStyle(null, mPrevFlag, mStyleFlag);
    }

    private String processStyleTextStyle(String text) {
        return processStyleTextStyle(text, mPrevFlag, mStyleFlag);
    }

    /**
     * Iterate through flags and check the styles that is set then apply to text
     * @return String
     */
    private String processStyleTextStyle(String text, int prevFlag, int styleFlag) {
        String tags = "";
        boolean isClosed = false;

        // iterate through flag list for closing
        for (int i = 0; i < mFlags.length; i++) {

            int flag = mFlags[i];

            if (prevFlag != styleFlag && (prevFlag & flag) == flag) {
                tags = tags + "" + equivHtmlTag(flag, true);
                isClosed = true;
            }
        }

        // iterate through flag list for opening
        for (int i = 0; i < mFlags.length; i++) {

            int flag = mFlags[i];

            if ((styleFlag & flag) == flag && (prevFlag & flag) == flag && !isClosed) {
                // no changes on current and previous styles
                // do nothing
            } else if ((styleFlag & flag) == flag) {
                // previous flags has the particular style flag and current flags has no particular style flag
                tags = tags + "" + equivHtmlTag(flag, false);
            }
        }

        if (text == null) {
            return tags;
        }

        return String.format("%1$s%2$s", tags, text);
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
                return !isClose ? "<span style=\"text-decoration:line-through;\">" : "</span>";
            case STYLE_COLOR_BLUE:
                return !isClose ? "<span style=\"color:#0000FF;\">" : "</span>";
            case STYLE_COLOR_BLACK:
                return !isClose ? "<span style=\"color:#000000;\">" : "</span>";
            case STYLE_COLOR_RED:
                return !isClose ? "<span style=\"color:#FF0000;\">" : "</span>";
            default:
                return "";
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (!mIsTextProcessing) {
            applyTextStyle(text, lengthBefore, lengthAfter);
        }
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

    public int[] getStyles() {
        return mFlags;
    }

    private String alterTags(String htmlText) {
        // remove all html tags except that contains span, b, i
        Pattern pattern = Pattern.compile("(<\\/?(?:span|b|i)[^>]*>)|<[^>]+>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        htmlText = matcher.replaceAll("$1");  // The substituted value will be passed to htmlText

        // remove all html tags except that contains img
        pattern = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(htmlText);

        htmlText = matcher.replaceAll(""); // The substituted value will be passed to htmlText

        htmlText = htmlText.replaceAll("(\r\n|\n)", "<br />"); // replace line breaks with br tag

        return htmlText;
    }
}

