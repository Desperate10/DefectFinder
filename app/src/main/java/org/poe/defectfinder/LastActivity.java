package org.poe.defectfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.poe.defectfinder.databinding.ActivityLastBinding;

import java.util.ArrayList;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class LastActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityLastBinding binding;
    private String opr, modificator;
    private ArrayList<String> previousDefects;
    private int type, oprOrSpan, objId;
    private String tabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLastBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        // Android 16 no longer calls onBackPressed() for apps targeting it: back goes through the dispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle !=null) {
            objId = bundle.getInt("objId");
            previousDefects = bundle.getStringArrayList("defect");
            tabel = bundle.getString("tabel");
            modificator = bundle.getString("mod");
            opr = bundle.getString("opr");
            //Log.d("zash", opr);
            type = bundle.getInt("type");
            oprOrSpan = bundle.getInt("oprOr");
            if (modificator.equals(Default.CONFIRM_MODIFICATOR)) {
                binding.addDefectBtn.setText(getString(R.string.add_fixed_defect_to_current_opr));
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) binding.addDefectBtn.getLayoutParams();
                layoutParams.height = (int) getResources().getDisplayMetrics().density * 150;
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                binding.addDefectBtn.setLayoutParams(layoutParams);
                //binding.addDefectBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 100));
                binding.addOpr.setText(R.string.add_fixed_defect_new_opr);
                binding.addOpr.setLayoutParams(layoutParams);
                binding.saveList.setText(R.string.save_fixed_list);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_defect_btn:
                Intent intent = new Intent(LastActivity.this, DefectActivity.class);
                intent.putExtra("objId", objId);
                intent.putExtra("mod", modificator);
                intent.putExtra("tabel", tabel);
                intent.putExtra("defect", true);
                intent.putExtra("previous_defect", previousDefects);
                intent.putExtra("opr", opr);
                intent.putExtra("type", type);
                intent.putExtra("oprOr", oprOrSpan);
                startActivity(intent);
                finish();
                break;
            case R.id.add_opr:
                if (type != R.raw.tp && type != R.raw.kl) {
                    Intent intent1 = new Intent(LastActivity.this, AddOprToLine.class);
                    intent1.putExtra("objId", objId);
                    intent1.putExtra("mod", modificator);
                    intent1.putExtra("tabel", tabel);
                    intent1.putExtra("type", type);
                    startActivity(intent1);
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.cant_add_opr_tp), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.save_list:
                Intent intent2 = new Intent(LastActivity.this, WatchCreatedLists.class);
                intent2.putExtra("mod", modificator);
                intent2.putExtra("tabel", tabel);
                startActivity(intent2);
                finish();
                break;
        }
    }

    private void goBack() {
        finish();
        Toast.makeText(this, getString(R.string.save_list), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LastActivity.this, WatchCreatedLists.class);
        intent.putExtra("mod", modificator);
        intent.putExtra("tabel", tabel);
        startActivity(intent);
        finish();
    }
}