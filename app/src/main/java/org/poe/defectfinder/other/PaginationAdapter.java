package org.poe.defectfinder.other;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;

import org.poe.defectfinder.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class PaginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private Context context;
    private List<Sheet> defectsList;
    private static final int LOADING = 0;
    private static final int ITEM = 1;
    private boolean isLoadingAdded = false;
    private ItemClickListener mOnEntryClickListener;

    public PaginationAdapter(Context context) {
        this.context = context;
        defectsList = new ArrayList<>();
    }

    public void setDefectsList(ArrayList<Sheet> defectsList) {
        this.defectsList = defectsList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

       // switch (viewType) {
      //      case ITEM:
                View viewItem = inflater.inflate(R.layout.sheet_item, parent, false);
                viewHolder = new DefectsViewHolder(viewItem);
        //        break;
        //    case LOADING:
         //       View viewLoading = inflater.inflate(R.layout.item_progress, parent, false);
        //        viewHolder = new LoadingViewHolder(viewLoading);
         //       break;
        //}
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Sheet sheet = defectsList.get(position);
        //Log.d("testim", sheet.toString());
       // switch (getItemViewType(position)) {
        //    case ITEM:
                DefectsViewHolder defectsViewHolder = (DefectsViewHolder) holder;
                defectsViewHolder.setTxtLineName(sheet.getLinename());
                defectsViewHolder.setTxtObhod(sheet.getObhod());
                defectsViewHolder.setTxtStatus(sheet.getStatus());

                List<Opora> opr = sheet.getOpr();
                defectsViewHolder.ll.removeAllViews();
                //for ( int i = 0; i < opr.size(); i++) {
                    Opora opora = opr.get(0);
                    defectsViewHolder.ll.setHasTransientState(true);
                    addOpr(context, defectsViewHolder, opora.getOpr(), opora.getDefect().get(0));
                //}
                defectsViewHolder.ll.setHasTransientState(false);

               // defectsViewHolder.setText(movie.getTitle());
        //        break;

        //    case LOADING:
        //        LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                //loadingViewHolder.progressBar.setVisibility(View.VISIBLE);
         //       break;
      //  }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    void addOpr(Context context, PaginationAdapter.DefectsViewHolder holder, String opr, Defect defect) {
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
        //for (int i = 0; i < defects.size(); i++) {
            TextView defectT = new TextView(context);
            defectT.setText(defect.getDefect());
            linearLayout.addView(defectT);
            TextView photos = new TextView(context);
            photos.setGravity(Gravity.RIGHT);
            photos.setText(""+defect.getPhoto().size());
            photoLayout.addView(photos);
        //}
        tableRow.addView(opora);
        tableRow.addView(linearLayout);
        tableRow.addView(photoLayout);
        holder.ll.addView(tableRow);
    }

    @Override
    public int getItemCount() {
        return defectsList == null ? 0 : defectsList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new Sheet());
    }

    public void removeLoadingFooter() {
        isLoadingAdded = false;

        int position = defectsList.size() - 1;
        Sheet result = getItem(position);

        if (result != null) {
            defectsList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void add(Sheet sheet) {
        defectsList.add(sheet);
        notifyItemInserted(defectsList.size() - 1);
    }

    public void addAll(ArrayList<Sheet> sheetResults) {
        for (Sheet result : sheetResults) {
            add(result);
        }
    }

    public void removeAll() {
        defectsList.clear();
        notifyDataSetChanged();
    }

    public Sheet getItem(int position) {
        return defectsList.get(position);
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onLongClick(View view, int position);
    }

    public void setOnItemClickListener(PaginationAdapter.ItemClickListener onEntryClickListener) {
        mOnEntryClickListener = onEntryClickListener;
    }

    private class DefectsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private TextView txtLineName;
        private TextView txtObhod;
        private TextView txtStatus;
        private LinearLayout ll;
        private TableRow defectRow;

        public DefectsViewHolder(View itemView) {
            super(itemView);

            txtLineName = itemView.findViewById(R.id.txt_short_name);
            txtObhod = itemView.findViewById(R.id.obhod);
            txtStatus = itemView.findViewById(R.id.status);
            ll = itemView.findViewById(R.id.linear_layout);
            defectRow = ll.findViewById(R.id.defect_row);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
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
                mOnEntryClickListener.onItemClick(v, getLayoutPosition());
            }
        }


        @Override
        public boolean onLongClick(View v) {
            if (mOnEntryClickListener != null) {
                mOnEntryClickListener.onLongClick( v, getLayoutPosition());
            }
            return true;
        }
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder {

        private ProgressBar progressBar;


        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.loadmore_progress);
        }
    }
}
