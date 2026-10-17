package ru.opds.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AppSettingsActivity extends Activity implements View.OnClickListener {

    private static final int REQ_FOLDER = 10;

    private EditText inputDir;
    private Button btnSave;
    private Button btnBrowse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_settings);

        inputDir = (EditText) findViewById(R.id.input_dir);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnBrowse = (Button) findViewById(R.id.btn_browse);

        AppSettingsStore settings = new AppSettingsStore(this);
        inputDir.setText(settings.getDownloadDir());

        btnSave.setOnClickListener(this);
        btnBrowse.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_browse) {
            Intent i = new Intent(this, FolderPickerActivity.class);
            i.putExtra("current", inputDir.getText().toString().trim());
            startActivityForResult(i, REQ_FOLDER);
        } else if (id == R.id.btn_save) {
            String path = inputDir.getText().toString().trim();
            if (path.length() == 0) {
                Toast.makeText(this, R.string.error_dir, Toast.LENGTH_LONG).show();
                return;
            }
            AppSettingsStore settings = new AppSettingsStore(this);
            settings.setDownloadDir(path);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FOLDER && resultCode == RESULT_OK && data != null) {
            String dir = data.getStringExtra("dir");
            if (dir != null) {
                inputDir.setText(dir);
            }
        }
    }
}