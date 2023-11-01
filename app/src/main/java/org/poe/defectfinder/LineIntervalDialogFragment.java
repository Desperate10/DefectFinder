package org.poe.defectfinder;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class LineIntervalDialogFragment extends DialogFragment {

    public interface OnInputIntervalListener {
        void sendInterval(int position, String start, String end);
    }

    public OnInputIntervalListener mOnInputIntervalListener;
    private int position;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        position = getArguments().getInt("position");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.dialog_interval_input, container, false);
        view.findViewById(R.id.acceptIntervalBtn).setOnClickListener(v -> {
            String start = ((EditText) view.findViewById(R.id.firstOprEditText)).getText().toString();
            String end = ((EditText) view.findViewById(R.id.lastOprEditText)).getText().toString();
            if ((start.isEmpty() && !end.isEmpty()) || (!start.isEmpty() && end.isEmpty())) {
                Toast.makeText(getActivity(), "Заповніть всі поля", Toast.LENGTH_SHORT).show();
            } else {
                mOnInputIntervalListener.sendInterval(position, start, end);
                dismiss();
            }
        });
        view.findViewById(R.id.dismissBtn).setOnClickListener(v -> getDialog().dismiss());
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mOnInputIntervalListener = (LineIntervalDialogFragment.OnInputIntervalListener) getActivity();
        } catch (ClassCastException e) {
            Log.e("dialog", "onAttach: ClassCastException: "
                    + e.getMessage());
        }
    }

    public static LineIntervalDialogFragment newInstance(int position) {
        LineIntervalDialogFragment fragment = new LineIntervalDialogFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        fragment.setArguments(args);
        return fragment;
    }
}
