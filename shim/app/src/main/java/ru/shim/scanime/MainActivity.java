package ru.shim.scanime;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public class MainActivity extends Activity {
    SharedPreferences p;
    EditText a, e, b;
    CheckBox en;
    RadioButton rc, rp;
    TextView last;

    EditText field(LinearLayout l, String label, String v) {
        TextView t = new TextView(this); t.setText(label); l.addView(t);
        EditText x = new EditText(this); x.setText(v); x.setSingleLine(); l.addView(x);
        return x;
    }
    Button btn(LinearLayout l, String t, View.OnClickListener c) {
        Button x = new Button(this); x.setText(t); x.setOnClickListener(c); l.addView(x); return x;
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        p = getSharedPreferences("c", 0);
        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setPadding(24, 24, 24, 24);
        sv.addView(l);

        btn(l, "1. Включить клавиатуру Scan Shim", v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        btn(l, "2. Выбрать её активной", v -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());

        a = field(l, "Broadcast action (из BROADCAST INTENT SETUP на Атоле)", p.getString("action", "com.hht.scanwedge"));
        e = field(l, "String extra", p.getString("extra", "com.hht.datawedge.data_string"));
        b = field(l, "Bytes extra", p.getString("bytes", "scandata_array"));

        RadioGroup rg = new RadioGroup(this);
        rc = new RadioButton(this); rc.setText("Режим: вставить текстом (commit)"); rc.setId(1);
        rp = new RadioButton(this); rp.setText("Режим: через буфер обмена (как Миндео)"); rp.setId(2);
        rg.addView(rc); rg.addView(rp); l.addView(rg);
        (p.getString("mode", "commit").equals("paste") ? rp : rc).setChecked(true);

        en = new CheckBox(this); en.setText("Нажимать Enter после кода");
        en.setChecked(p.getBoolean("enter", true)); l.addView(en);

        btn(l, "Сохранить", v -> {
            p.edit().putString("action", a.getText().toString().trim())
                .putString("extra", e.getText().toString().trim())
                .putString("bytes", b.getText().toString().trim())
                .putString("mode", rp.isChecked() ? "paste" : "commit")
                .putBoolean("enter", en.isChecked()).apply();
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        });

        TextView h = new TextView(this); h.setText("\nПоследний принятый скан:"); l.addView(h);
        last = new TextView(this); l.addView(last);
        btn(l, "Обновить", v -> show());
        setContentView(sv);
    }

    void show() { last.setText(p.getString("last", "—")); }
    @Override protected void onResume() { super.onResume(); show(); }
}
