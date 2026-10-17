package ru.opds.client;

public class Base64 {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static String encode(byte[] data) {
        StringBuilder out = new StringBuilder((data.length + 2) / 3 * 4);
        int i = 0;
        int len = data.length;
        while (i < len) {
            int b0 = data[i++] & 0xff;
            int b1 = i < len ? data[i++] & 0xff : -1;
            int b2 = i < len ? data[i++] & 0xff : -1;
            out.append(ALPHABET[b0 >> 2]);
            out.append(ALPHABET[((b0 & 0x03) << 4) | (b1 == -1 ? 0 : (b1 >> 4))]);
            out.append(b1 == -1 ? '=' : ALPHABET[((b1 & 0x0f) << 2) | (b2 == -1 ? 0 : (b2 >> 6))]);
            out.append(b2 == -1 ? '=' : ALPHABET[b2 & 0x3f]);
        }
        return out.toString();
    }
}