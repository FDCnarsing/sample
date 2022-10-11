package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

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
    private HashMap<Integer, Integer> mTextMap;
    private onStyleChangedListener mOnStyleChangedListener;
    private int[] mFlags = {STYLE_COLOR_RED, STYLE_COLOR_BLACK, STYLE_COLOR_BLUE, STYLE_STRIKETHROUGH, STYLE_ITALIC,STYLE_BOLD};
    private int[] oneStyleFlags = {STYLE_COLOR_BLUE, STYLE_COLOR_BLACK, STYLE_COLOR_RED}; // flags that can't be set to multiple
    private int mTextPos = 0;

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
        addTextChangedListener();
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

                        prevFlag = flags;
                    }

                    newText = alterTags(newText);

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
                    mPrevFlag = getPrevStyle();
                    newText =  processStyleTextStyle() + alterTags(replaceSpace(addedText));
                    mTextMap.put(text.length() - 1, mStyleFlag);
                } else {
                    // addedText is greater than 1, occurs when a paste happens
                    mTextMap.clear(); // clear textmap
                    mPrevTextHtml = "";
                    mPrevFlag = 0;
                    mStyleFlag = 0;
                    mTextPos = 0;

                    htmlText = alterTags(htmlText);

                    Document doc = Jsoup.parse(htmlText);

                    Elements elements = doc.body().children();

                    for (Element element : elements) {
                        getStylesFromElement(element);
                    }

                    newText = removeUnecessaryTags(htmlText);
                }
            }

            addText(newText, true);

            mPrevTextHtml += newText;
            mPrevText = Html.fromHtml(mPrevTextHtml).toString();
            mPrevFlag = mStyleFlag;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void getStylesFromElement(Element element) {
        String tag;
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                String textNode = ((TextNode) node).toString();
                if(textNode.length() > 1) {
                    for (int i = 0; i < textNode.length(); i++) {
                        // no style
                        mTextMap.put(mTextPos, mStyleFlag);
                        mTextPos++;
                    }
                } else {
                    mTextMap.put(mTextPos, mStyleFlag);
                    mTextPos++;
                }
                mStyleFlag = 0;
            } else if (node instanceof Element) {
                Element element1 = (Element)node;
                addStyleFlag(getEquivStyleFlagByElement(element1));
                getStylesFromElement(element1);
            }
        }
    }

    private void addText(String text, boolean isHtml) {

        if (getText() == null) {
            return;
        }

        Editable et = getText();

        if (isHtml) {

            // remove spans first
            Object[] toRemoveSpans = et.getSpans(0, et.length(), ForegroundColorSpan.class);
            for (int i = 0; i < toRemoveSpans.length; i++) {
                et.removeSpan(toRemoveSpans[i]);
            }

            // replace text
            getText().replace(0, getText().length(), Html.fromHtml(mPrevTextHtml +""+text));
            return;
        }

        getText().replace(0, getText().length(), String.format("%1$s%2$s",mPrevText, text));
    }

    private int getPrevStyle() {
        if (mTextMap == null || mTextMap.size() <= 0) {
            return 0;
        }

        int lastIndex = mTextMap.size()-1;

        if (mTextMap.containsKey(lastIndex)) {
            return mTextMap.get(lastIndex);
        }

        return 0;
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

    private int getEquivStyleFlagByElement(Element element) {
        if (element.tagName().equals("span")) {
            if (element.attr("style").contains("text-decoration:line-through")) {
                return STYLE_STRIKETHROUGH;
            } else if (element.attr("style").contains("color:#0000FF;")) {
                return STYLE_COLOR_BLUE;
            } else if (element.attr("style").contains("color:#000000;")) {
                return STYLE_COLOR_BLACK;
            } else if (element.attr("style").contains("color:#FF0000;")) {
                return STYLE_COLOR_RED;
            }
        } else if (element.tagName().equals("i")) {
            return STYLE_ITALIC;
        } else if (element.tagName().equals("b")) {
            return STYLE_BOLD;
        }

        return 0;
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
        if (!htmlText.matches("[\\n\\r]+") && htmlText.endsWith("\n")) {
            // if text is not only consist of line breaks and text ends with line breaks remove the trailing line break
            htmlText = htmlText.substring(0, htmlText.length()-1);
        }

        htmlText = htmlText.replace("\n", "<br/>");// replace line breaks with br tag

        return htmlText;
    }

    private String removeUnecessaryTags(String htmlText) {
        // remove all html tags except that contains span, b, i
        Pattern pattern = Pattern.compile("(<\\/?(?:span|b|i)[^>]*>)|<[^>]+>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        htmlText = matcher.replaceAll("$1");  // The substituted value will be passed to htmlText

        // remove all html tags except that contains img
        pattern = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(htmlText);

        htmlText = matcher.replaceAll(""); // The substituted value will be passed to htmlText
        return htmlText;
    }

    private String replaceSpace(String htmlText) {
        htmlText = htmlText.replaceAll(" ", "&nbsp;");

        return htmlText;
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            removeTextChangedListener();
            applyTextStyle(charSequence, i1, i2);
            addTextChangedListener();

        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void removeTextChangedListener() {
        removeTextChangedListener(mTextWatcher);
    }

    private void addTextChangedListener() {
        addTextChangedListener(mTextWatcher);
    }

}

