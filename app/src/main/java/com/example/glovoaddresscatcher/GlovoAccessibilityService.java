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

            Accessibility
