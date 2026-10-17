package ru.opds.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FolderPickerActivity extends Activity implements
        AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String ROOT = "/sdcard";

    private TextView pathView;
    private ListView list;
    private Button navUp;
    private Button choose;
    private ArrayAdapter<String> adapter;

    private File currentDir;
    private List<File> dirs = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.folder_picker);

        pathView = (TextView) findViewById(R.id.path);
        list = (ListView) findViewById(R.id.list);
        navUp = (Button) findViewById(R.id.btn_nav_up);
        choose = (Button) findViewById(R.id.btn_choose);
        navUp.setOnClickListener(this);
        choose.setOnClickListener(this);

        adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.title,
                new ArrayList<String>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);

        String initial = getIntent().getStringExtra("current");
        File start = new File(ROOT);
        if (initial != null && initial.startsWith(ROOT)) {
            File f = new File(initial);
            if (f.isDirectory()) start = f;
        }
        currentDir = start;
        update();
    }

    private void update() {
        pathView.setText(currentDir.getAbsolutePath());
        dirs.clear();
        File[] children = currentDir.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f.isDirectory() && !f.getName().startsWith(".")) {
                    dirs.add(f);
                }
            }
        }
        Collections.sort(dirs, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        adapter.clear();
        for (File f : dirs) {
            adapter.add(f.getName());
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File next = dirs.get(position);
        if (next.isDirectory()) {
            currentDir = next;
            update();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_nav_up) {
            File parent = currentDir.getParentFile();
            if (parent != null && parent.isDirectory()) {
                currentDir = parent;
                update();
            }
        } else if (id == R.id.btn_choose) {
            Intent result = new Intent();
            result.putExtra("dir", currentDir.getAbsolutePath());
            setResult(RESULT_OK, result);
            finish();
        }
    }
}