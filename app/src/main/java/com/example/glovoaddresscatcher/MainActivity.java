package com.example.glovoaddresscatcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.DecimalFormat;

public class MainActivity extends Activity {
    private LinearLayout root, content;
    private SharedPreferences prefs;
    private boolean shift;
    private long shiftStart;
    private final int BG=Color.rgb(8,12,16), CARD=Color.rgb(18,24,29), GREEN=Color.rgb(54,220,120), YELLOW=Color.rgb(255,205,30), MUTED=Color.rgb(145,155,165);

    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("glovo_capture",MODE_PRIVATE); shift=prefs.getBoolean("shift",false); shiftStart=prefs.getLong("shift_start",0); showHome();}
    @Override protected void onResume(){super.onResume(); if(content!=null) showHome();}

    private TextView tv(String s,int sp,int color){ TextView v=new TextView(this); v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setPadding(8,8,8,8);return v; }
    private TextView bold(String s,int sp,int color){TextView v=tv(s,sp,color);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(24,20,24,20);l.setBackgroundColor(CARD); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,18);l.setLayoutParams(p);return l;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(20,150,80));return b;}

    private void base(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(28,28,28,20); ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);}

    private void showHome(){base();
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL); head.addView(bold("⌖  Glovo Catcher",23,Color.WHITE),new LinearLayout.LayoutParams(0,-2,1)); TextView gear=bold("⚙",25,Color.WHITE);gear.setOnClickListener(v->showSettings());head.addView(gear);content.addView(head);
        LinearLayout status=card(); status.addView(bold(isServiceEnabled()?"✓  CATCHER АКТИВЕН":"!  CATCHER ВЫКЛЮЧЕН",18,isServiceEnabled()?GREEN:YELLOW));status.addView(tv(isServiceEnabled()?"Сервис работает в фоне":"Нажми, чтобы включить доступ",13,MUTED));status.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));content.addView(status);

        content.addView(bold("СЕГОДНЯ",14,MUTED)); LinearLayout stats=card(); LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER); row.addView(stat("Время работы",workTime()),new LinearLayout.LayoutParams(0,-2,1));row.addView(stat("Заработано",money()+" zł"),new LinearLayout.LayoutParams(0,-2,1));row.addView(stat("Почасовка",hourly()+" zł/ч"),new LinearLayout.LayoutParams(0,-2,1));stats.addView(row); Button shiftBtn=btn(shift?"ЗАВЕРШИТЬ СМЕНУ":"НАЧАТЬ СМЕНУ");shiftBtn.setOnClickListener(v->{shift=!shift;if(shift){shiftStart=System.currentTimeMillis();prefs.edit().putLong("shift_start",shiftStart).apply();}prefs.edit().putBoolean("shift",shift).apply();showHome();});stats.addView(shiftBtn);content.addView(stats);

        content.addView(bold("ТЕКУЩИЙ ЗАКАЗ",14,MUTED));String address=prefs.getString("last_address","Адрес ещё не пойман"); LinearLayout order=card();order.addView(bold("📍  "+address,18,Color.WHITE));order.addView(tv("Последний адрес, автоматически считанный из Glovo",12,MUTED));LinearLayout metrics=new LinearLayout(this);metrics.addView(metric("ДО КЛИЕНТА","— км",YELLOW),new LinearLayout.LayoutParams(0,-2,1));metrics.addView(metric("ТУДА И ОБРАТНО","— км",GREEN),new LinearLayout.LayoutParams(0,-2,1));metrics.addView(metric("ОПЛАТА","— zł",Color.WHITE),new LinearLayout.LayoutParams(0,-2,1));order.addView(metrics);Button maps=btn("ОТКРЫТЬ В КАРТАХ");maps.setOnClickListener(v->{if(!address.startsWith("Адрес")){Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="+Uri.encode(address)));startActivity(i);}});order.addView(maps);content.addView(order);

        LinearLayout today=card();today.addView(bold("СВОДКА",14,MUTED));today.addView(tv("Заказы  —     Пробег  —     Средний заказ  —",15,Color.WHITE));content.addView(today);
        TextView debug=tv("Диагностика захвата",13,MUTED);debug.setOnClickListener(v->showCapture());content.addView(debug);
    }

    private LinearLayout stat(String label,String value){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.addView(bold(value,18,GREEN));l.addView(tv(label,11,MUTED));return l;}
    private LinearLayout metric(String a,String b,int c){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.addView(tv(a,10,MUTED));l.addView(bold(b,17,c));return l;}
    private String workTime(){if(!shift||shiftStart==0)return "0 ч 00 мин";long m=(System.currentTimeMillis()-shiftStart)/60000;return (m/60)+" ч "+String.format("%02d",m%60)+" мин";}
    private String money(){return new DecimalFormat("0.00").format(prefs.getFloat("earnings",0));}
    private String hourly(){if(!shift||shiftStart==0)return "0.00";double h=(System.currentTimeMillis()-shiftStart)/3600000.0;return h<.02?"0.00":new DecimalFormat("0.00").format(prefs.getFloat("earnings",0)/h);}
    private boolean isServiceEnabled(){String s=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return s!=null&&s.toLowerCase().contains(getPackageName().toLowerCase());}

    private void showSettings(){base();content.addView(bold("‹  Настройки",23,Color.WHITE));LinearLayout c=card();c.addView(bold("Основные",15,Color.WHITE));c.addView(tv("• Автоматический захват адреса из Glovo\n• Работа в фоне\n• Открытие адреса в картах\n• Таймер смены и почасовка",15,Color.WHITE));content.addView(c);Button a=btn("НАСТРОЙКИ СПЕЦ. ВОЗМОЖНОСТЕЙ");a.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));content.addView(a);TextView back=tv("← Назад",16,GREEN);back.setPadding(8,30,8,20);back.setOnClickListener(v->showHome());content.addView(back);}
    private void showCapture(){base();content.addView(bold("Последний снимок Glovo",21,Color.WHITE));content.addView(tv(prefs.getString("last_capture","Пока данных нет. Открой экран заказа Glovo."),14,Color.WHITE));TextView back=tv("← Назад",16,GREEN);back.setOnClickListener(v->showHome());content.addView(back);}
}
