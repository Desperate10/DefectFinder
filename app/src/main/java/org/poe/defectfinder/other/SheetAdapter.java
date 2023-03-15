package org.poe.defectfinder.other;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.poe.defectfinder.R;
import org.poe.defectfinder.databinding.SheetRecyclerViewBinding;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SheetAdapter extends RecyclerView.Adapter<SheetAdapter.SheetHolder> {

    SheetRecyclerViewBinding binding;

    // List to store all details
    private ArrayList<Sheet> sheetsList;
    private Context mContext;
    private OnEntryClickListener mOnEntryClickListener;

    public SheetAdapter(ArrayList<Sheet> sheetsList, Context mContext) {
        this.sheetsList = sheetsList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public SheetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = SheetRecyclerViewBinding.inflate(LayoutInflater.from(parent.getContext()));


        // Inflate the layout view you have created for the list rows here
        //View view = layoutInflater.inflate(R.layout.sheet_recycler_view, parent, false);
        return new SheetHolder(binding.getRoot());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull SheetHolder holder, int position) {
        final Sheet sheet = sheetsList.get(position);

        // Set the data to the views here
        holder.setTxtLineName(sheet.getLinename());
        //holder.setTxtOpr(sheet.getOpr());
        holder.setTxtObhod(sheet.getObhod());
        holder.setTxtStatus(sheet.getStatus());



        List<Opora> opr = sheet.getOpr();
        holder.ll.removeAllViews();
        for ( int i = 0; i < opr.size(); i++) {
            Opora opora = opr.get(i);
            holder.ll.setHasTransientState(true);
            addOpr(mContext, holder, opora.getOpr(), opora.getDefect());
           // Defect defect = defects.get(i);
           // holder.ll.setHasTransientState(true);
           // addDefects(mContext, holder, defect.getDefect(), defect.getPhoto().size());
            //holder.defectRow.addView(getItemTextView(mContext, defect.getDefect()));
            //holder.defectRow.addView(getItemTextView(mContext, defect.getPhoto().size()));
        }
        holder.ll.setHasTransientState(false);

        // You can set click listners to indvidual items in the viewholder here
        // make sure you pass down the listner or make the Data members of the viewHolder public
    }

    void addOpr(Context context, SheetHolder holder, String opr, List<Defect> defects) {
        TableRow tableRow = new TableRow(context);
        TextView opora = new TextView(tableRow.getContext());
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.1f);
        opora.setLayoutParams(layoutParams);
        opora.setText(opr);
        LinearLayout linearLayout = new LinearLayout(tableRow.getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.8f));
        LinearLayout photoLayout = new LinearLayout(tableRow.getContext());
        photoLayout.setOrientation(LinearLayout.VERTICAL);
        photoLayout.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 0.1f));
        for (int i = 0; i < defects.size(); i++) {
            TextView defect = new TextView(context);
            defect.setText(defects.get(i).getDefect());
            linearLayout.addView(defect);
            TextView photos = new TextView(context);
            photos.setGravity(Gravity.RIGHT);
            photos.setText(""+defects.get(i).getPhoto().size());
            photoLayout.addView(photos);
        }
        tableRow.addView(opora);
        tableRow.addView(linearLayout);
        tableRow.addView(photoLayout);
        holder.ll.addView(tableRow);
    }

    @Override
    public int getItemCount() {
        return sheetsList == null? 0: sheetsList.size();
    }

    public interface OnEntryClickListener {
        void onEntryClick(View view, int position);
    }

    public void setOnEntryClickListener(OnEntryClickListener onEntryClickListener) {
        mOnEntryClickListener = onEntryClickListener;
    }

    // This is your ViewHolder class that helps to populate data to the view
    public class SheetHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView txtLineName;
        private TextView txtObhod;
        private TextView txtStatus;
        private LinearLayout ll;
        private TableRow defectRow;

        public SheetHolder(View itemView) {
            super(itemView);

            txtLineName = itemView.findViewById(R.id.txt_short_name);
            txtObhod = itemView.findViewById(R.id.obhod);
            txtStatus = itemView.findViewById(R.id.status);
            ll = itemView.findViewById(R.id.linear_layout);
            defectRow = ll.findViewById(R.id.defect_row);

            itemView.setOnClickListener(this);
        }

        public void setTxtLineName(String lineName) {
            txtLineName.setText(lineName);
        }

        public void setTxtObhod(String obhod) {
            txtObhod.setText(obhod);
        }

        public void setTxtStatus(String status) {txtStatus.setText(status);}


        @Override
        public void onClick(View v) {
            if (mOnEntryClickListener != null) {
                mOnEntryClickListener.onEntryClick(v, getLayoutPosition());
            }
        }


    }
}
