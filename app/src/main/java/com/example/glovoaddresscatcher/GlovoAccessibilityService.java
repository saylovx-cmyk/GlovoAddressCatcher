package com.example.glovoaddresscatcher;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GlovoAccessibilityService extends AccessibilityService {

    private WindowManager windowManager;
    private TextView overlay;
    private String lastSavedText = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showOverlay("Catcher включён ✓");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null || event.getPackageName() == null) {
            return;
        }

        String packageName = event.getPackageName()
                .toString()
                .toLowerCase(Locale.ROOT);

        /*
         * Работаем только с Glovo / Rider.
         */
        if (!packageName.contains("glovo")
                && !packageName.contains("rider")
                && !packageName.contains("logistics")) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root == null) {
            return;
        }

        List<String> texts = new ArrayList<>();

        collectText(root, texts);

        StringBuilder all = new StringBuilder();

        for (String text : texts) {
            if (!text.trim().isEmpty()) {
                all.append(text.trim()).append("\n");
            }
        }

        String captured = all.toString().trim();

        if (captured.isEmpty()) {
            return;
        }

        /*
         * Сохраняем последний снимок интерфейса Glovo.
         */
        getSharedPreferences("glovo_capture", MODE_PRIVATE)
                .edit()
                .putString("last_capture", captured)
                .apply();

        String candidate = findAddressCandidate(texts);

        if (candidate != null && !candidate.equals(lastSavedText)) {

            lastSavedText = candidate;

            getSharedPreferences("glovo_capture", MODE_PRIVATE)
                    .edit()
                    .putString("last_address", candidate)
                    .apply();

            showOverlay("📍 " + candidate);
        }
    }

    private void collectText(
            AccessibilityNodeInfo node,
            List<String> result
    ) {

        if (node == null) {
            return;
        }

        /*
         * Обычный видимый текст.
         */
        if (node.getText() != null) {

            String text = node.getText()
                    .toString()
                    .trim();

            if (!text.isEmpty() && !result.contains(text)) {
                result.add(text);
            }
        }

        /*
         * Очень важно:
         * карты и маркеры иногда хранят адрес именно здесь.
         */
        if (node.getContentDescription() != null) {

            String description = node.getContentDescription()
                    .toString()
                    .trim();

            if (!description.isEmpty()
                    && !result.contains(description)) {

                result.add(description);
            }
        }

        /*
         * Иногда Glovo может спрятать дополнительный текст
         * как hint.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (node.getHintText() != null) {

                String hint = node.getHintText()
                        .toString()
                        .trim();

                if (!hint.isEmpty() && !result.contains(hint)) {
                    result.add(hint);
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {

    AccessibilityNodeInfo child = node.getChild(i);

    if (child != null) {
        collectText(child, result);
        child.recycle();
    }
}
}

/*
 * Пока это тестовый фильтр.
 * После первого реального заказа подправим его под Glovo.
 */
private String findAddressCandidate(List<String> texts) {

    for (String text : texts) {

        String lower = text.toLowerCase(Locale.ROOT);

        boolean hasStreetWord =
                lower.contains("ul.")
                || lower.contains("ulica")
                || lower.contains("aleja")
                || lower.contains("plac")
                || lower.contains("osiedle")
                || lower.contains("święty")
                || lower.contains("św.");

        boolean hasNumber = text.matches(".*\\d+.*");

        boolean reject =
                lower.contains("zł")
                || lower.contains("km")
                || lower.contains("zamów")
                || lower.contains("заказ")
                || lower.contains("доставка")
                || lower.contains("delivery")
                || lower.contains("принять")
                || lower.contains("accept");

        if (!reject
                && text.length() >= 5
                && text.length() <= 120
                && (hasStreetWord || hasNumber)) {

            return text;
        }
    }

    return null;
}

private void showOverlay(String text) {

    if (windowManager == null) {
        return;
    }

    if (overlay == null) {

        overlay = new TextView(this);
        overlay.setTextSize(16);
        overlay.setTextColor(0xFFFFFFFF);
        overlay.setBackgroundColor(0xE6000000);
        overlay.setPadding(25, 15, 25, 15);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        try {
            windowManager.addView(overlay, params);
        } catch (Exception ignored) {
            return;
        }
    }

    overlay.setText(text);
}

@Override
public void onInterrupt() {
}

@Override
public void onDestroy() {

    if (overlay != null && windowManager != null) {
        try {
            windowManager.removeView(overlay);
        } catch (Exception ignored) {
        }
    }

    overlay = null;
    super.onDestroy();
}
}
