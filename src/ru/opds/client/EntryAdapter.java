package ru.opds.client;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class EntryAdapter extends BaseAdapter {
    private final Context context;
    private List<OpdsParser.Entry> entries;

    public EntryAdapter(Context context, List<OpdsParser.Entry> entries) {
        this.context = context;
        this.entries = entries;
    }

    public void setEntries(List<OpdsParser.Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entries == null ? 0 : entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.row, parent, false);
        }
        OpdsParser.Entry e = entries.get(position);

        TextView title = (TextView) v.findViewById(R.id.title);
        TextView author = (TextView) v.findViewById(R.id.author);
        TextView desc = (TextView) v.findViewById(R.id.desc);
        View authorRow = v.findViewById(R.id.author_row);
        View dividerAuthor = v.findViewById(R.id.divider_author);
        View descBlock = v.findViewById(R.id.desc_block);

        if (e.isFeed) {
            title.setText("> " + e.title);
        } else {
            title.setText(e.title);
        }

        if (e.author != null && e.author.length() > 0) {
            authorRow.setVisibility(View.VISIBLE);
            dividerAuthor.setVisibility(View.VISIBLE);
            author.setText(e.author);
        } else {
            authorRow.setVisibility(View.GONE);
            dividerAuthor.setVisibility(View.GONE);
        }

        if (e.description != null && e.description.length() > 0) {
            descBlock.setVisibility(View.VISIBLE);
            desc.setText(e.description);
        } else {
            descBlock.setVisibility(View.GONE);
        }

        return v;
    }
}