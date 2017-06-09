package de.tellfee.meinfangbuch.adapter;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.tellfee.meinfangbuch.R;
import de.tellfee.meinfangbuch.model.Fischart;

/**
 * Created by Lukas Brinkmann on 09.06.2017.
 */

public class FischartLVAdapter extends BaseAdapter{
    private ArrayList<Fischart> items;
    private Context context;

    public FischartLVAdapter(Context context, ArrayList<Fischart> items){
        this.items      = items;
        this.context    = context;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Fischart getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.basic_list_view, parent, false);
        }

        Fischart currentItem    = getItem(position);
        TextView item_name      = (TextView)convertView.findViewById(R.id.text_view_item_name);

        item_name.setText(currentItem.getName());

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.basic_list_view_item, parent, false);
        }

        Fischart currentItem    = getItem(position);
        TextView item_name      = (TextView)convertView.findViewById(R.id.text_view_item_name);

        item_name.setText(currentItem.getName());

        return convertView;
    }
}
