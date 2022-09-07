package com.example.myapplication;

import android.content.Context;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.EmojiSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomTextStyleEditTextView extends AppCompatEditText {

    private static int emojiCnt = 0;
    private boolean containsEmoji = false;
    private boolean nars = false;
    private boolean narsing =false;
    private boolean selectMoreThanOne = false;

    public interface OnStyleChangedListener{
        void onStyleChange(List<Integer> styles);
    }

    public final static int STYLE_STRIKETHROUGH = 1;
    public final static int STYLE_BOLD = 2;
    public final static int STYLE_ITALIC = 4;
    public final static int STYLE_COLOR_BLUE = 8;
    public final static int STYLE_COLOR_BLACK = 16;
    public final static int STYLE_COLOR_RED = 32;

    private final static int[] styles = {STYLE_STRIKETHROUGH,STYLE_BOLD,STYLE_ITALIC,STYLE_COLOR_BLUE,STYLE_COLOR_BLACK,STYLE_COLOR_RED};
    private OnStyleChangedListener mOnStyleChangedListener;

    private HashMap<Integer, Integer> mTextMap;
    private int mTextStyle = 0;

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
        mTextStyle = 0;
    }

    public void setTextStyle(List<Integer> flags) {
        mTextStyle = 0;
        for (int flag : flags) {
            mTextStyle = mTextStyle | flag;
        }
    }


    private void applyTextStyle(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (narsing){
            narsing = false;
            return;
        }
        if(lengthAfter > lengthBefore) { // new text added
            CharSequence textAdded = text.subSequence(start,start + lengthAfter);
            int position = start; // position is index where new text was added in the original text

            // Split hash map
            HashMap<Integer, Integer> beforeMap = new HashMap<>();
            HashMap<Integer, Integer> afterMap = new HashMap<>();
            boolean isExistInMainMap = false;
            for(Map.Entry<Integer, Integer> entry : mTextMap.entrySet()) {
                int key = entry.getKey();
                int flag = entry.getValue();
                if(key == position) isExistInMainMap = true;
                if(isExistInMainMap) {
                    afterMap.put(key,flag);
                } else {
                    beforeMap.put(key,flag);
                }
            }

            // Generate map from text added
            HashMap<Integer, Integer> newMap = new HashMap<>();
            int tempPos = position;
            for(int i=0 ;i<textAdded.length();i++) {
                newMap.put(tempPos, mTextStyle);
                tempPos++;
            }

            // Append beforeTextMap + newMap + afterMap
            int interval = lengthAfter - lengthBefore;

            for(Map.Entry<Integer, Integer> entry : beforeMap.entrySet()) {
                int key = entry.getKey();
                int flag = entry.getValue();
                mTextMap.put(key,flag);
            }
            for(Map.Entry<Integer, Integer> entry : newMap.entrySet()) {
                int key = entry.getKey();
                int flag = entry.getValue();
                mTextMap.put(key,flag);
            }
            for(Map.Entry<Integer, Integer> entry : afterMap.entrySet()) {
                int key = entry.getKey();
                int flag = entry.getValue();
                mTextMap.put(key+interval,flag);
            }

            processTextStyle(text,position + 1);
        } else if(lengthBefore > lengthAfter) { // some text are removed
            int lengthOfRemovedItems = lengthBefore - lengthAfter;
            int previousSize = mTextMap.size() - lengthOfRemovedItems;
            // Remove all map objects
            for(int i =start;i<start+lengthOfRemovedItems;i++) {
                mTextMap.remove(i);
            }
            // shift all remaining object
            for(int i=start+lengthOfRemovedItems;i<previousSize;i++) {
                if(mTextMap.containsKey(i)) {
                    int value = mTextMap.get(i);
                    mTextMap.put(i - lengthOfRemovedItems,value);
                    mTextMap.remove(i);
                }
            }
        }

    }

    private void processTextStyle(CharSequence text,int position) {
        Log.d("CheckValue", "TextMap1 : " + mTextMap);
        String temp = "",resultHtml = "";
        for(int i=0; i<text.length();i++) {
            char c = text.charAt(i);
            try {
                int cStyle = mTextMap.get(i) == null ? 0 : mTextMap.get(i);
                int cNextStyle = mTextMap.get(i+1) == null ? 0 : mTextMap.get(i+1);
                temp = temp + c;
                if(!(cNextStyle == cStyle && cNextStyle != 0)) {
                    if(temp.isEmpty()) temp = Character.toString(c);
                    resultHtml = resultHtml + equivHtml(temp,cStyle);
                    temp = "";
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            int type = Character.getType(text.charAt(position-1));
            String letter = String.valueOf(text.charAt(position-1));
            Pattern digit = Pattern.compile("[0-9]");
            Pattern special = Pattern.compile ("[!₱/@:;`#$%&*()_+=|<>?{}\\[\\]~-]");
            Matcher hasDigit = digit.matcher(letter);
            Matcher hasSpecial = special.matcher(letter);
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {

                return;
            }
            else if (hasDigit.find() || hasSpecial.find()) {
//                Spanned htmlText = Html.fromHtml(resultHtml);
                if (getText().length() > 0){
                    nars = true;
                    narsing = true;
                    getText().clear();
                    narsing = true;
                    append(Html.fromHtml(resultHtml));
                    setSelection(position);
                    Log.d("NARSING", "NISUD NARS: resultHtml1 " + resultHtml + " letter1: " + letter);
                }

//                return;
                nars = false;
            }
            else{
                if (nars){
                    nars = false;
                    return;
                }
                setText(Html.fromHtml(resultHtml));
                Log.d("NARSING", "NISUD NARS: resultHtml2 " + resultHtml + " letter2: " + letter);
                if(position <= text.length()) setSelection(position);
            }
        }
        catch (Exception ex) {
            Toast.makeText(getContext(),ex.toString(), Toast.LENGTH_LONG).show();
        }
//        setText(Html.fromHtml(resultHtml));
//        Log.d("NARSING", "NISUD NARS: resultHtml2 " + resultHtml);
//        if(position <= text.length()) setSelection(position);
    }

    private String equivHtml(String temp, int style) {
        String result = temp;
        result = result.replaceAll(" ","&nbsp;");
        result = result.replaceAll("(\r\n|\n)", "<br/>");
        for(int i=0;i<styles.length;i++) {
            if((styles[i] & style) != 0) {
                int toApplyStyle = styles[i];
                switch(toApplyStyle) {
                    case STYLE_STRIKETHROUGH:
                        result = "<s>" + result + "</s>";
                        break;
                    case STYLE_BOLD:
                        result = "<b>" + result + "</b>";
                        break;
                    case STYLE_ITALIC:
                        result = "<i>" + result + "</i>";
                        break;
                    case STYLE_COLOR_BLACK:
                        result = "<font color='#000000'>" + result + "</font>";
                        break;
                    case STYLE_COLOR_BLUE:
                        result = "<font color='#0000ff'>" + result + "</font>";
                        break;
                    case STYLE_COLOR_RED:
                        result = "<font color='#ff0000'>" + result + "</font>";
                        break;
                }
            }
        }
        return result;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {

        applyTextStyle(text,start,lengthBefore,lengthAfter);
//            super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        // Change of current style
        if(mTextMap != null) {
            List<Integer> result = new ArrayList<>();
            int style = 0;
            if(mTextMap.containsKey(selStart-1)) {
                style = mTextMap.get(selStart-1);
            }

            for(int i=0;i<styles.length;i++) {
                if((styles[i] & style) != 0) {
                    int toApplyStyle = styles[i];
                    result.add(toApplyStyle);
                }
            }
            this.mOnStyleChangedListener.onStyleChange(result);
//            selectMoreThanOne = false;
//            if (selStart - selEnd > 0){
//                selectMoreThanOne = true;
//            }
        }
    }

    public void setOnStyleChangeListener(OnStyleChangedListener listener) {
        this.mOnStyleChangedListener = listener;
    }
}

