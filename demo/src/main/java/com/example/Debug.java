package com.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.function.Supplier;

public class Debug {
    private static boolean enabled = true;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ANSI 顏色定義
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";  // DEBUG
    private static final String CYAN = "\u001B[96m";   // DUMP
    private static final String BLUE = "\u001B[94m";   // VAR
    private static final String YELLOW = "\u001B[93m"; // WARNING
    private static final String RED = "\u001B[91m";    // ERROR

    // 建立監聽器介面 (給UI使用的日誌)
    public interface LogListener {
        void onMessage(String msg);
    }

    private static LogListener uiListener;

    public static void setUiListener(LogListener l) {
        uiListener = l;
    }

    private Debug() {
        throw new AssertionError("這是一個設定檔，不能被實體化！");
    }

    private static void write(String color, String label, String msg) {
        System.out.printf("%s[%s %s %s]%s %s%n", color, label, getTime(), getTraceString(), RESET, msg);

        if (uiListener != null) {
            String uiMsg = String.format("[%s] %s", label, msg);
            uiListener.onMessage(uiMsg);
        }
    }

    private static String getTraceString() {
        return StackWalker.getInstance()
                        .walk(frames -> frames
                        .filter(f -> !f.getClassName().equals(Debug.class.getName()))
                        .filter(f -> !f.getClassName().contains("java.lang"))
                        .map(f -> {
                            String fileName = (f.getFileName() != null) ? f.getFileName() : "Unknown";
                            return String.format("%s:%d(%s)", fileName, f.getLineNumber(), f.getMethodName());
                        })
                        .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                            java.util.Collections.reverse(list);
                            return String.join(" -> ", list);
                        })));
    }

    private static String getTime() {
        return LocalTime.now()
                        .format(TIME_FORMAT);
    }

    private static String formatArgs(Object... args) {
        if (args == null || args.length == 0) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
                sb.append(args[i]);
            if (i < args.length - 1) sb.append(" "); // 只在中間加空格
        }

        return sb.toString();
    }

    public static void log(Object... args) {
        if (enabled) write(GREEN, "LOG", formatArgs(args));
    }

    public static void var(String name, Object value) {
        if (enabled) write(BLUE, "VAR", name + " = " + value);
    }
    public static void var(String name, Supplier<Object> value) {
        if (enabled) write(BLUE, "VAR", name + " = " + value.get());
    }

    public static void dump(Object data, String label) {
        if (!enabled) return;
        String prefix = (label != null) ? "(" + label + ") " : "";
        // dump 比較特殊，數據通常佔多行，所以獨立處理格式
        String msg = prefix + "\n" + (data != null ? data.toString() : "null");
        write(CYAN, "DUMP", msg);
    }

    public static void warn(Object... args) {
        if (enabled) write(YELLOW, "WARNING", formatArgs(args));
    }

    public static void error(Object... args) {
        write(RED, "ERROR", formatArgs(args));
    }

    public static <T> T requireNoNull(T obj, Supplier<String> message) {
        if (obj == null) {
            String errorMsg = (message != null ? message.get() : "Error message missing");

            error(errorMsg);

            throw new NullPointerException(errorMsg);
        }
        return obj;
    }

    public static void setEnabled(boolean status) { enabled = status; }
}
