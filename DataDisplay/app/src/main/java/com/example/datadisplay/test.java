package com.example.datadisplay;

public class test {

    // Escape JSON special characters
    public static String removeQuotes(String text) {
        if (text == null) return null;
        return text.replace("\"", ""); // removes all " characters
    }

    // ✅ Main method for testing
    public static void main(String[] args) {
        String raw = "";
        String cleaned = removeQuotes(raw);

        System.out.println("Before: " + raw);
        System.out.println("After:  " + cleaned);
    }
}