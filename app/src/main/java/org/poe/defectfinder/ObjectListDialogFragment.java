package org.poe.defectfinder;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ObjectListDialogFragment extends DialogFragment {

    // Оновлений інтерфейс з requestCode
    public interface OnInputListener {
        void sendInput(String input, int requestCode);
    }

    // Константи для розрізнення спіннерів
    public static final int REQUEST_CODE_GROUP = 1;
    public static final int REQUEST_CODE_DEFECT = 2;
    public static final int REQUEST_CODE_SUBGROUP = 3;

    public OnInputListener mOnInputListener;
    private List<String> objects;
    private String hint;
    private int requestCode; // Додали поле для зберігання коду запиту

    private EditText mInput;
    private ListView mList;
    private TextView dialogHint;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        objects = getArguments().getStringArrayList("OBJECTS");
        hint = getArguments().getString("HINT");
        requestCode = getArguments().getInt("REQUEST_CODE", 0); // Читаємо код запиту
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.dialog_searchable_spinner, container, false);

        mInput = view.findViewById(R.id.input);
        mList = view.findViewById(R.id.list_view);
        dialogHint = view.findViewById(R.id.dialog_hint_tv);
        dialogHint.setText(hint);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<>(objects));
        mList.setAdapter(adapter);

        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mList.setOnItemClickListener((parent, view1, position, id) -> {
            String input = mList.getItemAtPosition(position).toString();
            // Передаємо input разом з requestCode
            mOnInputListener.sendInput(input, requestCode);
            dismiss();
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mOnInputListener = (OnInputListener) getActivity();
        } catch (ClassCastException e) {
            Log.e("dialog", "onAttach: ClassCastException: " + e.getMessage());
        }
    }

    // Оновлений метод newInstance з requestCode
    public static ObjectListDialogFragment newInstance(ArrayList<String> objects, String hint, int requestCode) {
        ObjectListDialogFragment fragment = new ObjectListDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("OBJECTS", objects);
        args.putString("HINT", hint);
        args.putInt("REQUEST_CODE", requestCode); // Додаємо код запиту
        fragment.setArguments(args);
        return fragment;
    }

    // Додатковий метод для зворотної сумісності (якщо десь використовується старий варіант)
    @Deprecated
    public static ObjectListDialogFragment newInstance(ArrayList<String> objects, String hint) {
        return newInstance(objects, hint, 0);
    }
}
