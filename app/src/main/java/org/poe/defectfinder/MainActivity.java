package org.poe.defectfinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.poe.defectfinder.databinding.ActivityMainBinding;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;
import org.poe.defectfinder.network.Response;
import org.poe.defectfinder.other.CustomLoginDialog;
import org.poe.defectfinder.update.UpdatePrompt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityMainBinding binding;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private String filial;
    private MyApi api;
    private UpdatePrompt updatePrompt;
    String tabel, token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.versionLabel.setText("v" + BuildConfig.VERSION_NAME);
        updatePrompt = new UpdatePrompt(this);

        api = ClientApi.getInstance(this).getApi();

        sharedPreferences = getSharedPreferences("filial", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");
        api.checkTokenValidity(token).enqueue(new Callback<org.poe.defectfinder.network.Response>() {
            @Override
            public void onResponse(Call<org.poe.defectfinder.network.Response> call, retrofit2.Response<Response> response) {
                if(response.code() == 200) {
                    // Log.d("tokenyk", response.body().getError()+ " " +response.body().getMessage());
                    // Skip if the dialog was already restored after recreation, or the screen is being saved
                    if (response.body().getError().equals("true")
                            && !getSupportFragmentManager().isStateSaved()
                            && getSupportFragmentManager().findFragmentByTag("login_dialog") == null) {
                        CustomLoginDialog customLoginDialog = new CustomLoginDialog();
                        customLoginDialog.setCancelable(false);
                        customLoginDialog.show(getSupportFragmentManager(), "login_dialog");
                    }
                }
            }

            @Override
            public void onFailure(Call<org.poe.defectfinder.network.Response> call, Throwable t) {

            }
        });
        checkFilial();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            tabel = bundle.getString("tabel");
            binding.tabel.setText(tabel);
        }
        backPressHandle();

    }

    @Override
    protected void onStart() {
        super.onStart();
        // play build: Google Play; direct build: the DefectFinder server
        updatePrompt.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        updatePrompt.stop();
    }

    private void checkFilial() {
        filial = sharedPreferences.getString("filial", "00");
        if (filial.equals("00")) {
            chooseYourFilialDialog();
        }
        updateFilialButton();
    }

    private void updateFilialButton() {
        String[] filials = getResources().getStringArray(R.array.filials);
        String displayName = filial;
        for (String f : filials) {
            if (f.startsWith(filial)) {
                displayName = f.trim();
                break;
            }
        }
        binding.changeFilial.setText(getString(R.string.change_filial) + ": " + displayName);
    }

    private void chooseYourFilialDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_filial, null);
        builder.setCancelable(false);
        builder.setView(dialogView);

        String[] filials = getResources().getStringArray(R.array.filials);

        final Spinner spinner = (Spinner) dialogView.findViewById(R.id.spinner);
        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filials);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        spinner.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button create = dialogView.findViewById(R.id.choose);
        create.setOnClickListener(view -> {
            filial = spinner.getSelectedItem().toString().split(" ")[0];
            editor = sharedPreferences.edit();
            editor.putString("filial", filial);
            editor.apply();
            alertDialog.cancel();
            updateFilialButton();
        });
    }

    @Override
    public void onClick(View v) {
        Intent intent;
            switch (v.getId()) {
                case R.id.button:
                    if (validate()) {
                        intent = new Intent(MainActivity.this, CreateListActivity.class);
                        intent.putExtra("mod", Default.ADD_MODIFICATOR);
                        intent.putExtra("tabel", binding.tabel.getEditableText().toString());
                        startActivity(intent);
                        finish();
                    }
                    break;
                case R.id.watch_lists:
                    intent = new Intent(MainActivity.this,WatchCreatedLists.class);
                    intent.putExtra("mod", Default.ADD_MODIFICATOR);
                    startActivity(intent);
                    finish();
                    break;
                case R.id.defect_ready_btn:
                    if (validate()) {
                        intent = new Intent(MainActivity.this, CreateListActivity.class);
                        intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                        intent.putExtra("tabel", binding.tabel.getEditableText().toString());
                        startActivity(intent);
                        finish();
                    }
                    break;
                case R.id.watch_ready_lists:
                    intent = new Intent(MainActivity.this,WatchCreatedLists.class);
                    intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                    startActivity(intent);
                    finish();
                    break;
                case R.id.watch_defects:
                    if(validate()) {
                        intent = new Intent(MainActivity.this, WatchDefects.class);
                        intent.putExtra("tabel", binding.tabel.getEditableText().toString());
                        //intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                        startActivity(intent);
                        finish();
                    }
                    break;
                case R.id.change_filial:
                    chooseYourFilialDialog();
                    break;
            }
    }

    /*private boolean checkTabel(String tabel) {
        boolean valid = false;
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.tabel);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            String lines;
            while ((lines = in.readLine()) != null) {
                if (lines.equals(tabel)) {
                    valid = true;
                }
            }

            in.close();
            inputStream.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return valid;
    }*/

    boolean validate () {
        boolean valid = true;

        String tabel = binding.tabel.getText().toString();

        if (tabel.isEmpty() || tabel.length() < 4 || tabel.length() > 6) {
            binding.tabel.setError(getString(R.string.enter_valid_table_error));
            valid = false;
        } /*else if(!checkTabel(tabel)){
            binding.tabel.setError(getString(R.string.no_tabel_error));
            valid = false;
        }*/ else {
            binding.tabel.setError(null);
        }
        return valid;
    }


    public void backPressHandle() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // same behavior as before
            }
        });
    }
}