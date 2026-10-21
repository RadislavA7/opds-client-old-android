package ru.opds.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class CatalogActivity extends Activity implements AdapterView.OnItemClickListener,
        View.OnClickListener, TextView.OnEditorActionListener {

    private static final String DOWNLOAD_DIR = "/sdcard/Books";

    private ListView list;
    private TextView empty;
    private ProgressBar progress;
    private EditText searchInput;
    private Button searchBtn;
    private Button refreshBtn;
    private EntryAdapter adapter;

    private String catalogName;
    private String currentUrl;
    private String login;
    private String password;
    private String searchTemplate;
    private String pendingPresetQuery;
    private List<OpdsParser.Entry> entries;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            progress.setVisibility(View.GONE);
            switch (msg.what) {
                case 0: // error
                    showError((String) msg.obj);
                    break;
                case 1: // loaded
                    render();
                    break;
                case 2: // download ok
                    Toast.makeText(CatalogActivity.this, R.string.download_started, Toast.LENGTH_LONG).show();
                    break;
                case 3: // download fail
                    showError(getString(R.string.download_failed));
                    break;
                case 4: // search template received
                    searchBtn.setEnabled(true);
                    searchInput.setEnabled(true);
                    if (pendingPresetQuery != null) {
                        String preset = pendingPresetQuery;
                        pendingPresetQuery = null;
                        searchInput.setText(preset);
                        doSearch();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.catalog);

        list = (ListView) findViewById(R.id.list);
        empty = (TextView) findViewById(R.id.empty);
        progress = (ProgressBar) findViewById(R.id.progress);
        searchInput = (EditText) findViewById(R.id.search_input);
        searchBtn = (Button) findViewById(R.id.search_btn);
        refreshBtn = (Button) findViewById(R.id.refresh_btn);
        searchInput.setOnEditorActionListener(this);
        searchBtn.setOnClickListener(this);
        refreshBtn.setOnClickListener(this);
        searchInput.setEnabled(false);
        searchBtn.setEnabled(false);

        adapter = new EntryAdapter(this, new ArrayList<OpdsParser.Entry>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);

        catalogName = getIntent().getStringExtra("name");
        currentUrl = getIntent().getStringExtra("url");
        login = getIntent().getStringExtra("login");
        password = getIntent().getStringExtra("password");
        if (login == null) login = "";
        if (password == null) password = "";
        setTitle(catalogName);

        String autoQuery = getIntent().getStringExtra("query");
        if (autoQuery != null && autoQuery.length() > 0) {
            load(currentUrl);
            // searchTemplate arrives via message 4, so run search after load completes
            pendingPresetQuery = autoQuery;
        } else {
            load(currentUrl);
        }
        // debug/testing hook: start a book download immediately (adb am start --es download <url>)
        String autoDownload = getIntent().getStringExtra("download");
        if (autoDownload != null && autoDownload.length() > 0) {
            final String dl = autoDownload;
            final String fn = getIntent().getStringExtra("name") != null
                    ? getIntent().getStringExtra("name") + ".epub"
                    : "book.epub";
            handler.postDelayed(new Runnable() {
                public void run() {
                    download(dl, fn);
                }
            }, 2000);
        }
    }

    private void load(final String url) {
        entries = null;
        adapter.setEntries(new ArrayList<OpdsParser.Entry>());
        progress.setVisibility(View.VISIBLE);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpOp http = new HttpOp();
                http.setCredentials(login, password);
                try {
                    HttpOp.Result r = http.open(url);
                    OpdsParser.FeedInfo info = new OpdsParser().parseFeed(r.stream);
                    try { r.stream.close(); } catch (IOException ignore) {}
                    if (info == null) {
                        handler.sendMessage(handler.obtainMessage(0, getString(R.string.error_parse)));
                    } else {
                        if (info.searchTemplate != null) {
                            searchTemplate = info.searchTemplate;
                            if (!searchTemplate.startsWith("http://") && !searchTemplate.startsWith("https://")) {
                                if (searchTemplate.startsWith("/")) {
                                    int hostEnd = url.indexOf("/", url.indexOf("://") + 3);
                                    if (hostEnd >= 0) {
                                        searchTemplate = url.substring(0, hostEnd) + searchTemplate;
                                    }
                                } else {
                                    String baseDir = url;
                                    int q = baseDir.indexOf('?');
                                    if (q >= 0) baseDir = baseDir.substring(0, q);
                                    int slash = baseDir.lastIndexOf('/');
                                    searchTemplate = (slash >= 0 ? baseDir.substring(0, slash + 1) : baseDir) + searchTemplate;
                                }
                            }
                            handler.obtainMessage(4).sendToTarget();
                        }
                        resolveRelative(url, info.entries);
                        entries = info.entries;
                        handler.sendEmptyMessage(1);
                    }
                } catch (IOException e) {
                    handler.sendMessage(handler.obtainMessage(0, getString(R.string.error_network)));
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void resolveRelative(String base, List<OpdsParser.Entry> es) {
        String baseDir = base;
        int q = baseDir.indexOf('?');
        if (q >= 0) baseDir = baseDir.substring(0, q);
        int slash = baseDir.lastIndexOf('/');
        String dir = slash >= 0 ? baseDir.substring(0, slash + 1) : baseDir;
        for (OpdsParser.Entry e : es) {
            String baseForEntry = computeBase(base, e.link, dir);
            if (e.link.startsWith("http://") || e.link.startsWith("https://")) {
                // keep absolute; still resolve format links against the absolute entry link
                resolveFormats(baseForEntry, e);
                continue;
            }
            if (e.link.startsWith("/")) {
                int hostEnd = base.indexOf("/", base.indexOf("://") + 3);
                if (hostEnd >= 0) {
                    e.link = base.substring(0, hostEnd) + e.link;
                }
            } else {
                e.link = dir + e.link;
            }
            resolveFormats(e.link, e);
            if (e.formats != null) {
                for (OpdsParser.Format f : e.formats) {
                    android.util.Log.e("OPDSClient", "resolved format '" + e.title + "': " + f.link);
                }
            }
        }
    }

    /** Resolves relative links of book formats against the entry's (now absolute) link. */
    private void resolveFormats(String entryLink, OpdsParser.Entry e) {
        if (e.formats == null) return;
        String eb = entryLink;
        if (!eb.startsWith("http://") && !eb.startsWith("https://")) {
            return;
        }
        int hq = eb.indexOf('?');
        if (hq >= 0) eb = eb.substring(0, hq);
        int lslash = eb.lastIndexOf('/');
        String eDir = lslash >= 0 ? eb.substring(0, lslash + 1) : eb + "/";
        for (OpdsParser.Format f : e.formats) {
            if (f.link == null || f.link.startsWith("http://") || f.link.startsWith("https://")) {
                continue;
            }
            if (f.link.startsWith("/")) {
                int hostEnd = eb.indexOf("/", eb.indexOf("://") + 3);
                if (hostEnd >= 0) {
                    f.link = eb.substring(0, hostEnd) + f.link;
                }
            } else {
                f.link = eDir + f.link;
            }
        }
    }

    /** Chooses the base for resolving: the entry's own absolute URL if present, else catalog base. */
    private String computeBase(String catalogBase, String entryLink, String dir) {
        if (entryLink != null && (entryLink.startsWith("http://") || entryLink.startsWith("https://"))) {
            return entryLink;
        }
        return catalogBase;
    }

    private void render() {
        android.util.Log.e("OPDSClient", "render: entries=" + (entries == null ? "null" : entries.size()) + " template=" + searchTemplate);
        if (entries == null || entries.size() == 0) {
            progress.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            empty.setText(getString(R.string.empty_feed));
            return;
        }
        empty.setVisibility(View.GONE);
        adapter.setEntries(entries);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final OpdsParser.Entry e = entries.get(position);
        if (e.isFeed) {
            progress.setVisibility(View.VISIBLE);
            currentUrl = e.link;
            load(e.link);
        } else if (e.isBook) {
            confirmDownload(e);
        }
    }

    private void confirmDownload(final OpdsParser.Entry e) {
        String dirPath = new AppSettingsStore(this).getDownloadDir();
        String msg = getString(R.string.download_dir) + "\n" + dirPath;

        if (e.formats != null && e.formats.size() > 1) {
            final java.util.ArrayList<String> labels = new java.util.ArrayList<String>();
            for (OpdsParser.Format f : e.formats) {
                labels.add(formatName(f.type));
            }
            new AlertDialog.Builder(this)
                    .setTitle(e.title)
                    .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            OpdsParser.Format f = e.formats.get(which);
                            download(f.link, f.fileName);
                        }
                    })
                    .show();
            return;
        }

        String link = e.link;
        String fileName = e.fileName;
        if (e.formats != null && e.formats.size() == 1) {
            OpdsParser.Format f = e.formats.get(0);
            link = f.link;
            fileName = f.fileName;
        }
        final String flink = link;
        final String fname = fileName;
        new AlertDialog.Builder(this)
                .setTitle(e.title)
                .setMessage(msg)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        download(flink, fname);
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private static String formatName(String type) {
        if (type == null) return "Файл";
        String t = type.toLowerCase();
        if (t.contains("epub")) return "EPUB";
        if (t.contains("mobi")) return "MOBI";
        if (t.contains("azw")) return "AZW";
        if (t.contains("fb2")) return "FB2";
        if (t.contains("pdf")) return "PDF";
        if (t.contains("txt")) return "TXT";
        if (t.contains("html")) return "HTML";
        return "Файл";
    }

    private void download(final String link, final String fileName) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.loading));
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();

        final String dirPath = new AppSettingsStore(this).getDownloadDir();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(dirPath);
                    if (!dir.exists()) dir.mkdirs();
                    String fn = fileName != null && fileName.length() > 0 ? fileName : "book";
                    File dest = new File(dir, fn);
                    HttpOp http = new HttpOp();
                    http.setCredentials(login, password);
                    http.downloadToFile(link, dest);
                    handler.removeMessages(3);
                    handler.sendEmptyMessage(2);
                } catch (IOException ex) {
                    handler.sendMessage(handler.obtainMessage(3));
                } finally {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (pd.isShowing()) pd.dismiss();
                        }
                    });
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showError(String message) {
        empty.setVisibility(View.VISIBLE);
        empty.setText(message);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.search_btn) {
            doSearch();
        } else if (id == R.id.refresh_btn) {
            if (currentUrl != null && currentUrl.length() > 0) {
                load(currentUrl);
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
            doSearch();
            return true;
        }
        return false;
    }

    private void doSearch() {
        if (searchTemplate == null) {
            android.util.Log.e("OPDSClient", "search: no template");
            showError(getString(R.string.no_search));
            return;
        }
        String q = searchInput.getText().toString().trim();
        if (q.length() == 0) {
            return;
        }
        String url;
        try {
            url = searchTemplate.replace("{searchTerms}", URLEncoder.encode(q, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            url = searchTemplate.replace("{searchTerms}", q);
        }
        android.util.Log.e("OPDSClient", "search url=" + url);
        currentUrl = url;
        load(url);
    }

    @Override
    public boolean onSearchRequested() {
        return super.onSearchRequested();
    }
}