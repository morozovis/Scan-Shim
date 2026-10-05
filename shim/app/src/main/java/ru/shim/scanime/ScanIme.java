package ru.shim.scanime;

import android.content.*;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScanIme extends InputMethodService {
    private BroadcastReceiver rx;
    private SharedPreferences p;

    @Override public void onCreate() { super.onCreate(); p = getSharedPreferences("c", 0); reg(); }
    @Override public void onDestroy() { unreg(); super.onDestroy(); }
    @Override public void onStartInput(EditorInfo a, boolean r) { super.onStartInput(a, r); reg(); }

    private void unreg() { if (rx != null) { try { unregisterReceiver(rx); } catch (Exception e) {} rx = null; } }
    private void reg() {
        unreg();
        rx = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { handle(i); } };
        IntentFilter f = new IntentFilter(p.getString("action", "com.hht.scanwedge"));
        f.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(rx, f);
    }

    private String extract(Intent i) {
        String s = i.getStringExtra(p.getString("extra", "com.hht.datawedge.data_string"));
        if (s == null) {
            byte[] b = i.getByteArrayExtra(p.getString("bytes", "scandata_array"));
            if (b != null) s = new String(b, StandardCharsets.UTF_8);
        }
        if (s == null && i.getExtras() != null)
            for (String k : i.getExtras().keySet()) { Object o = i.getExtras().get(k); if (o instanceof String) { s = (String) o; break; } }
        return s;
    }

    private void key(InputConnection ic, int code) {
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void handle(Intent i) {
        String s = extract(i);
        if (s == null || s.isEmpty()) return;
        while (s.endsWith("\n") || s.endsWith("\r")) s = s.substring(0, s.length() - 1);
        int gs = 0; for (char ch : s.toCharArray()) if (ch == 29) gs++;
        p.edit().putString("last", new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())
            + " длина=" + s.length() + " GS=" + gs + "\n" + s.replace("\u001d", "<GS>")).apply();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) { p.edit().putString("last", p.getString("last", "") + "\n(нет поля ввода!)").apply(); return; }
        if ("paste".equals(p.getString("mode", "commit"))) {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("scan", s));
            ic.performContextMenuAction(android.R.id.paste);
        } else {
            ic.commitText(s, 1);
        }
        if (p.getBoolean("enter", true)) key(ic, KeyEvent.KEYCODE_ENTER);
    }

    private boolean ru = false, shift = false, sym = false;
    private static final String[][] EN = {{"q","w","e","r","t","y","u","i","o","p"},{"a","s","d","f","g","h","j","k","l"},{"z","x","c","v","b","n","m"}};
    private static final String[][] RU = {{"й","ц","у","к","е","н","г","ш","щ","з","х"},{"ф","ы","в","а","п","р","о","л","д","ж","э"},{"я","ч","с","м","и","т","ь","б","ю","ъ"}};
    private static final String[][] SM = {{"1","2","3","4","5","6","7","8","9","0"},{"-","/",":",";","(",")","@","\"","&","%"},{".",",","?","!","'","#","+","=","_","*"}};

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private Button kb(String t, float w, View.OnClickListener c) {
        Button b = new Button(this);
        b.setText(t); b.setAllCaps(false); b.setTextSize(18);
        b.setPadding(0, 0, 0, 0); b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setOnClickListener(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), w);
        lp.setMargins(2, 2, 2, 2);
        b.setLayoutParams(lp);
        return b;
    }

    private void refresh() { setInputView(build()); }

    private void type(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(shift ? s.toUpperCase() : s, 1);
        if (shift) { shift = false; refresh(); }
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(4, 4, 4, 4);
        LinearLayout top = new LinearLayout(this);
        TextView st = new TextView(this);
        st.setText("Scan Shim: ждёт скан");
        st.setTextSize(12);
        top.addView(st, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top);

        String[][] L = sym ? SM : (ru ? RU : EN);
        for (String[] row : L) {
            LinearLayout r = new LinearLayout(this);
            for (String k : row) r.addView(kb(shift && !sym ? k.toUpperCase() : k, 1, v -> type(k)));
            root.addView(r);
        }
        LinearLayout r = new LinearLayout(this);
        r.addView(kb(shift ? "⬆" : "⇧", 1.4f, v -> { shift = !shift; refresh(); }));
        r.addView(kb(sym ? "ABC" : "?123", 1.4f, v -> { sym = !sym; refresh(); }));
        r.addView(kb(ru ? "RU" : "EN", 1.2f, v -> { ru = !ru; sym = false; refresh(); }));
        r.addView(kb("пробел", 3.5f, v -> type(" ")));
        r.addView(kb("←", 1.4f, v -> { InputConnection ic = getCurrentInputConnection(); if (ic != null) key(ic, KeyEvent.KEYCODE_DEL); }));
        r.addView(kb("Ввод", 1.6f, v -> { InputConnection ic = getCurrentInputConnection(); if (ic != null) key(ic, KeyEvent.KEYCODE_ENTER); }));
        root.addView(r);
        return root;
    }

    @Override public View onCreateInputView() { return build(); }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        int c = info.inputType & InputType.TYPE_MASK_CLASS;
        boolean num = (c == InputType.TYPE_CLASS_NUMBER || c == InputType.TYPE_CLASS_PHONE);
        if (!restarting) { sym = num; shift = false; refresh(); }
    }
}
