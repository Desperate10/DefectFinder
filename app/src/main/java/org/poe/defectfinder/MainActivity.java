package org.poe.defectfinder;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.ActivityResult;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.poe.defectfinder.databinding.ActivityMainBinding;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;
import org.poe.defectfinder.network.Response;
import org.poe.defectfinder.other.CustomLoginDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_REQUEST_CODE = 100;
    ActivityMainBinding binding;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private String filial;
    private MyApi api;
    private AppUpdateManager appUpdateManager;
    String tabel, token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        api = ClientApi.getInstance(this).getApi();

        sharedPreferences = getSharedPreferences("filial", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");
        api.checkTokenValidity(token).enqueue(new Callback<org.poe.defectfinder.network.Response>() {
            @Override
            public void onResponse(Call<org.poe.defectfinder.network.Response> call, retrofit2.Response<Response> response) {
                if(response.code() == 200) {
                    // Log.d("tokenyk", response.body().getError()+ " " +response.body().getMessage());
                    if (response.body().getError().equals("true")) {
                        CustomLoginDialog customLoginDialog = new CustomLoginDialog(MainActivity.this);
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


    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUpdate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    private void checkUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.registerListener(installStateUpdatedListener);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE /*AppUpdateType.IMMEDIATE*/, MainActivity.this, MY_REQUEST_CODE);

                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }

            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED){
                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                popupSnackbarForCompleteUpdate();
            } else {
              //  Log.e(TAG, "checkForAppUpdateAvailability: something else");
            }
        });
    }

    InstallStateUpdatedListener installStateUpdatedListener = new
            InstallStateUpdatedListener() {
                @Override
                public void onStateUpdate(@NonNull InstallState state) {
                    if (state.installStatus() == InstallStatus.DOWNLOADED){
                        //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                        popupSnackbarForCompleteUpdate();
                    } else if (state.installStatus() == InstallStatus.INSTALLED){
                        if (appUpdateManager != null){
                            appUpdateManager.unregisterListener(installStateUpdatedListener);
                        }
                    } else {
                       // Log.i(TAG, "InstallStateUpdatedListener: state: " + state.installStatus());
                    }
                }
            };

    private void popupSnackbarForCompleteUpdate() {

        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.coord),
                        getString(R.string.new_version),
                        Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction(getString(R.string.install), view -> {
            if (appUpdateManager != null){
                appUpdateManager.completeUpdate();
            }
        });


        snackbar.setActionTextColor(getResources().getColor(R.color.design_default_color_on_secondary));
        snackbar.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, getString(R.string.update_error), Toast.LENGTH_SHORT).show();
                // If the update is cancelled or fails,
                // you can request to start the update again.
            } else if (ActivityResult.RESULT_IN_APP_UPDATE_FAILED == requestCode) {
                checkUpdate();
            }

        }
    }

    private void checkFilial() {
        filial = sharedPreferences.getString("filial", "00");
        if (filial.equals("00")) {
            chooseYourFilialDialog();
        }
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
            }
    }

    private boolean checkTabel(String tabel) {
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
    }

    boolean validate () {
        boolean valid = true;

        String tabel = binding.tabel.getText().toString();

        if (tabel.isEmpty() || tabel.length() < 4) {
            binding.tabel.setError(getString(R.string.enter_valid_table_error));
            valid = false;
        } else if(!checkTabel(tabel)){
            binding.tabel.setError(getString(R.string.no_tabel_error));
            valid = false;
        } else {
            binding.tabel.setError(null);
        }
        return valid;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}