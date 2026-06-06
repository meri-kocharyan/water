package com.example.water;

import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.EditText;

public class PasteableEditText extends androidx.appcompat.widget.AppCompatEditText {

    public PasteableEditText(Context context) {
        super(context);
    }

    public PasteableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PasteableEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            ClipboardManager clipboard = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                // Try to get HTML from the clipboard
                if (clipboard.getPrimaryClipDescription()
                        .hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                    String html = clipboard.getPrimaryClip()
                            .getItemAt(0)
                            .getHtmlText();
                    if (html != null) {
                        // Convert the full HTML to a simple Spanned, then to plain text with tags
                        Spanned spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
                        // Convert Spanned back to simple HTML tags (bold, italic, etc.)
                        String simpleHtml = convertSpannedToSimpleHtml(spanned);
                        insertSimpleHtml(simpleHtml);
                        return true;
                    }
                }
                // Fallback: plain text paste
            }
        }
        return super.onTextContextMenuItem(id);
    }

    // Converts a Spanned into a string with <b>, <i>, <u>, <br> tags only
    private String convertSpannedToSimpleHtml(Spanned spanned) {
        StringBuilder out = new StringBuilder();
        int len = spanned.length();
        int next;
        for (int i = 0; i < len; i = next) {
            next = spanned.nextSpanTransition(i, len, Object.class);
            Object[] spans = spanned.getSpans(i, next, Object.class);
            boolean bold = false, italic = false, underline = false;
            for (Object span : spans) {
                if (span instanceof android.text.style.StyleSpan) {
                    int style = ((android.text.style.StyleSpan) span).getStyle();
                    if (style == android.graphics.Typeface.BOLD) bold = true;
                    else if (style == android.graphics.Typeface.ITALIC) italic = true;
                } else if (span instanceof android.text.style.UnderlineSpan) {
                    underline = true;
                }
            }
            String chunk = spanned.subSequence(i, next).toString();
            if (chunk.equals("\n")) {
                out.append("<br>");
            } else {
                if (bold) out.append("<b>");
                if (italic) out.append("<i>");
                if (underline) out.append("<u>");
                out.append(chunk);
                if (underline) out.append("</u>");
                if (italic) out.append("</i>");
                if (bold) out.append("</b>");
            }
        }
        return out.toString();
    }

    // Inserts HTML at cursor position using Html.fromHtml (temporary)
    private void insertSimpleHtml(String html) {
        Spanned spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        int start = getSelectionStart();
        int end = getSelectionEnd();
        SpannableStringBuilder builder = new SpannableStringBuilder(getText());
        builder.replace(start, end, spanned);
        setText(builder);
        setSelection(start + spanned.length());
    }
}