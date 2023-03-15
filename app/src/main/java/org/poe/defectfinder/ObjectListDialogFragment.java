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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ObjectListDialogFragment extends DialogFragment {

    public interface OnInputListener {
        void sendInput(String input);
    }

    public OnInputListener mOnInputListener;
    private List<String> objects;

    private EditText mInput;
    private ListView mList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        objects = getArguments().getStringArrayList("OBJECTS");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.dialog_searchable_spinner, container, false);

        mInput = view.findViewById(R.id.input);
        mList = view.findViewById(R.id.list_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, objects);
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
            mOnInputListener.sendInput(input);
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
            Log.e("dialog", "onAttach: ClassCastException: "
                    + e.getMessage());
        }
    }

    public static ObjectListDialogFragment newInstance(ArrayList<String> objects) {
        ObjectListDialogFragment fragment = new ObjectListDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("OBJECTS", objects);
        fragment.setArguments(args);
        return fragment;
    }
}
