package org.poe.defectfinder.other;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.poe.defectfinder.R;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.LoginRequest;
import org.poe.defectfinder.network.LoginResponse;
import org.poe.defectfinder.network.MyApi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;


public class CustomLoginDialog extends DialogFragment {

    EditText login;
    EditText password;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    MyApi api;
    Context context;

    public CustomLoginDialog(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
       sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("filial", MODE_PRIVATE);

        api = ClientApi.getInstance(context).getApi();
        View view = getActivity().getLayoutInflater().inflate(R.layout.custom_login_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Аутентификація");
        builder.setView(view);
        setCancelable(false);

        login = view.findViewById(R.id.login);
        password = view.findViewById(R.id.password);

        builder.setPositiveButton("Вхід", (dialog, which) -> {});

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (!login.getText().toString().isEmpty() && !password.getText().toString().isEmpty()) {
                    api.login(new LoginRequest(login.getText().toString(), password.getText().toString())).enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            if (response.code() == 200) {
                                editor = sharedPreferences.edit();
                                editor.putString("token", response.body().jwt);
                                editor.apply();
                               // Toast.makeText(getActivity(), "Успешно "+response.body().jwt, Toast.LENGTH_LONG).show();
                                d.dismiss();
                            } else {
                                Toast.makeText(getActivity(), "Неправильний логін або пароль", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            Toast.makeText(getActivity(), "Помилка роботи сервису, спробуйте пізніше", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "Заповніть поля логін та пароль!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
