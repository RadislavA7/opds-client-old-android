package ru.opds.client;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BooksActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String DOWNLOAD_DIR = "/sdcard/Books";

    private ListView list;
    private TextView empty;
    private TextView desc;
    private ArrayAdapter<String> adapter;
    private List<File> files = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.books);

        list = (ListView) findViewById(R.id.list);
        empty = (TextView) findViewById(R.id.empty);
        desc = (TextView) findViewById(R.id.desc);

        adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.title,
                new ArrayList<String>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);

        desc.setText(getString(R.string.download_dir));
        refresh();
    }

    private void refresh() {
        adapter.clear();
        files.clear();
        String dirPath = new AppSettingsStore(this).getDownloadDir();
        desc.setText(getString(R.string.download_dir) + ": " + dirPath);
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] all = dir.listFiles();
            if (all != null) {
                for (File f : all) {
                    if (f.isFile()) {
                        files.add(f);
                        adapter.add(f.getName());
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        empty.setVisibility(files.size() == 0 ? View.VISIBLE : View.GONE);
        empty.setText(getString(R.string.empty_feed));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File f = files.get(position);
        if (f.exists()) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(f), mimeOf(f.getName()));
                startActivity(i);
            } catch (Exception e) {
                // no handler - ignore
            }
        }
    }

    private static String mimeOf(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".fb2")) return "application/x-fictionbook+xml";
        if (n.endsWith(".epub")) return "application/epub+zip";
        if (n.endsWith(".mobi")) return "application/x-mobipocket-ebook";
        if (n.endsWith(".azw")) return "application/vnd.amazon.ebook";
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".txt")) return "text/plain";
        if (n.endsWith(".html") || n.endsWith(".htm")) return "text/html";
        if (n.endsWith(".zip")) return "application/zip";
        return "*/*";
    }
}