package ru.opds.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity implements View.OnClickListener {

    private EditText inputName;
    private EditText inputUrl;
    private EditText inputLogin;
    private EditText inputPassword;
    private int index = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.catalog_edit);

        inputName = (EditText) findViewById(R.id.input_name);
        inputUrl = (EditText) findViewById(R.id.input_url);
        inputLogin = (EditText) findViewById(R.id.input_login);
        inputPassword = (EditText) findViewById(R.id.input_password);
        Button save = (Button) findViewById(R.id.btn_save);
        Button cancel = (Button) findViewById(R.id.btn_cancel);

        index = getIntent().getIntExtra("index", -1);
        if (index >= 0) {
            inputName.setText(getIntent().getStringExtra("name"));
            inputUrl.setText(getIntent().getStringExtra("url"));
            String login = getIntent().getStringExtra("login");
            String password = getIntent().getStringExtra("password");
            if (login != null) inputLogin.setText(login);
            if (password != null) inputPassword.setText(password);
        }

        save.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_save) {
            String name = inputName.getText().toString().trim();
            String url = inputUrl.getText().toString().trim();
            String login = inputLogin.getText().toString().trim();
            String password = inputPassword.getText().toString();
            if (name.length() == 0) {
                Toast.makeText(this, R.string.error_empty_name, Toast.LENGTH_LONG).show();
                return;
            }
            if (url.length() == 0) {
                Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_LONG).show();
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            Intent result = new Intent();
            result.putExtra("name", name);
            result.putExtra("url", url);
            result.putExtra("login", login);
            result.putExtra("password", password);
            result.putExtra("index", index);
            setResult(RESULT_OK, result);
            finish();
        } else if (id == R.id.btn_cancel) {
            finish();
        }
    }
}