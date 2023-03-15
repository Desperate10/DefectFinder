package org.poe.defectfinder.other;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import org.poe.defectfinder.R;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Image> data;
    private ArrayList<String> uri;
    private LayoutInflater inflater;

    public ImageAdapter(Context context, ArrayList<Image> data, ArrayList<String> uri) {
        this.context = context;
        this.data = data;
        this.uri = uri;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.image_grid_layout, null);
            holder = new Holder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.grid_item_image);
            holder.ivDelete = (ImageView) convertView.findViewById(R.id.iv_delete);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        try {
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.imageView.setImageURI(data.get(position).getURI());
        } catch (Exception e) {
            Toast.makeText(context, "Неможливе редактування ( помилка фото )", Toast.LENGTH_SHORT).show();
        }


        if (position == 0) {
            holder.ivDelete.setVisibility(View.GONE);
        } else {
            holder.ivDelete.setVisibility(View.VISIBLE);
        }


        holder.ivDelete.setOnClickListener(view -> {
            try {
                //тест нужен
                data.remove(position);
                uri.remove(position-1);
                context.getContentResolver().delete(data.get(position).getURI(), null, null);
            } catch (SecurityException e) {
                Toast.makeText(context, "Фото зроблені не з додатку потрібно видаляти з галереї власноруч", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {

            }

            notifyDataSetChanged();
        });
        return convertView;
    }


    class Holder {
        private ImageView imageView;
        private ImageView ivDelete;
    }
}