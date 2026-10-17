package ru.opds.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private CatalogStore store;
    private List<Catalog> catalogs;
    private ArrayAdapter<String> adapter;
    private ListView list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        store = new CatalogStore(this);
        catalogs = new ArrayList<Catalog>(store.load());

        list = (ListView) findViewById(R.id.list);
        empty = (TextView) findViewById(R.id.empty);
        Button add = (Button) findViewById(R.id.btn_add);
        Button books = (Button) findViewById(R.id.btn_books);
        Button settings = (Button) findViewById(R.id.btn_settings);

        adapter = new ArrayAdapter<String>(this, R.layout.row, R.id.title,
                new ArrayList<String>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        add.setOnClickListener(this);
        books.setOnClickListener(this);
        settings.setOnClickListener(this);

        refresh();
    }

    private void refresh() {
        adapter.clear();
        for (Catalog c : catalogs) {
            adapter.add(c.name);
        }
        adapter.notifyDataSetChanged();
        boolean show = catalogs.size() == 0;
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
        empty.setText(getString(R.string.empty_catalogs));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_add) {
            openEditor(-1);
        } else if (id == R.id.btn_books) {
            startActivity(new Intent(this, BooksActivity.class));
        } else if (id == R.id.btn_settings) {
            startActivity(new Intent(this, AppSettingsActivity.class));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Catalog c = catalogs.get(position);
        Intent i = new Intent(this, CatalogActivity.class);
        i.putExtra("name", c.name);
        i.putExtra("url", c.url);
        i.putExtra("login", c.login);
        i.putExtra("password", c.password);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        new AlertDialog.Builder(this)
                .setTitle(catalogs.get(position).name)
                .setItems(new String[]{getString(R.string.btn_edit), getString(R.string.btn_delete)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) {
                                    openEditor(position);
                                } else {
                                    confirmDelete(position);
                                }
                            }
                        })
                .show();
        return true;
    }

    private void openEditor(final int index) {
        Intent i = new Intent(this, SettingsActivity.class);
        if (index >= 0) {
            i.putExtra("index", index);
            i.putExtra("name", catalogs.get(index).name);
            i.putExtra("url", catalogs.get(index).url);
            i.putExtra("login", catalogs.get(index).login);
            i.putExtra("password", catalogs.get(index).password);
        }
        startActivityForResult(i, 1);
    }

    private void confirmDelete(final int index) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_delete))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        catalogs.remove(index);
                        store.save(catalogs);
                        refresh();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra("name");
            String url = data.getStringExtra("url");
            String login = data.getStringExtra("login");
            String password = data.getStringExtra("password");
            int index = data.getIntExtra("index", -1);
            if (index >= 0 && index < catalogs.size()) {
                catalogs.get(index).name = name;
                catalogs.get(index).url = url;
                catalogs.get(index).login = login == null ? "" : login;
                catalogs.get(index).password = password == null ? "" : password;
            } else {
                catalogs.add(new Catalog(name, url, login, password));
            }
            store.save(catalogs);
            refresh();
        }
    }
}