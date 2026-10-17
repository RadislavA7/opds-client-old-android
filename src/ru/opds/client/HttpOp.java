package ru.opds.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class HttpOp {

    public static class Result {
        public InputStream stream;
        public String finalUrl;
        public String contentType;
        public String contentDisposition;
        public int length;
    }

    private String login;
    private String password;

    public void setCredentials(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public Result open(String url) throws IOException {
        try {
            return openInternal(url);
        } catch (IOException e) {
            android.util.Log.e("OPDSClient", "open failed: " + url, e);
            throw e;
        }
    }

    private Result openInternal(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "OPDSClient/1.0");
        c.setRequestProperty("Accept-Encoding", "gzip");
        if (login != null && login.length() > 0) {
            String pair = login + ":" + (password == null ? "" : password);
            String b64 = Base64.encode(pair.getBytes("UTF-8"));
            c.setRequestProperty("Authorization", "Basic " + b64);
        }
        int code = c.getResponseCode();
        if (code >= 400) {
            c.disconnect();
            throw new IOException("HTTP " + code);
        }
        Result r = new Result();
        r.contentType = c.getContentType();
        r.contentDisposition = c.getHeaderField("Content-Disposition");
        r.length = c.getContentLength();
        InputStream raw = c.getInputStream();
        String enc = c.getContentEncoding();
        if (enc != null && enc.equalsIgnoreCase("gzip")) {
            raw = new GZIPInputStream(raw);
        }
        r.stream = new BufferedInputStream(raw);
        return r;
    }

    public void downloadToFile(String url, File dest) throws IOException {
        Result r = open(url);
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buf = new byte[16384];
            int n;
            while ((n = r.stream.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
            out.close();
        } finally {
            try { r.stream.close(); } catch (IOException ignore) {}
        }
    }
}