package ru.opds.client;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class CatalogStore {
    private static final String PREFS = "catalogs";
    private static final String KEY_NAMES = "names";
    private static final String KEY_URLS = "urls";
    private static final String KEY_LOGINS = "logins";
    private static final String KEY_PASSWORDS = "passwords";

    private final SharedPreferences prefs;

    public CatalogStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Catalog> load() {
        String names = prefs.getString(KEY_NAMES, "");
        String urls = prefs.getString(KEY_URLS, "");
        String logins = prefs.getString(KEY_LOGINS, "");
        String passwords = prefs.getString(KEY_PASSWORDS, "");
        String[] n = split(names);
        String[] u = split(urls);
        String[] l = split(logins);
        String[] p = split(passwords);
        List<Catalog> out = new ArrayList<Catalog>();
        for (int i = 0; i < n.length && i < u.length; i++) {
            if (n[i].length() > 0) {
                String login = i < l.length ? l[i] : "";
                String pass = i < p.length ? p[i] : "";
                out.add(new Catalog(n[i], u[i], login, pass));
            }
        }
        return out;
    }

    public void save(List<Catalog> catalogs) {
        StringBuilder nb = new StringBuilder();
        StringBuilder ub = new StringBuilder();
        StringBuilder lb = new StringBuilder();
        StringBuilder pb = new StringBuilder();
        for (int i = 0; i < catalogs.size(); i++) {
            if (i > 0) {
                nb.append('\u0001');
                ub.append('\u0001');
                lb.append('\u0001');
                pb.append('\u0001');
            }
            nb.append(catalogs.get(i).name);
            ub.append(catalogs.get(i).url);
            lb.append(catalogs.get(i).login);
            pb.append(catalogs.get(i).password);
        }
        prefs.edit()
                .putString(KEY_NAMES, nb.toString())
                .putString(KEY_URLS, ub.toString())
                .putString(KEY_LOGINS, lb.toString())
                .putString(KEY_PASSWORDS, pb.toString())
                .commit();
    }

    private static String[] split(String s) {
        if (s == null || s.length() == 0) {
            return new String[0];
        }
        return s.split("\u0001", -1);
    }
}