package tech.qiji.android.mupdf.custombookmark.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import tech.qiji.android.mupdf.OutlineItem;
import tech.qiji.android.mupdf.R;

/**
 * Created by Administrator on 19/10/2560.
 */

public class IndexBookAdapter extends RecyclerView.Adapter<IndexBookAdapter.IndexViewHolder> {

    Context mContext;
    OutlineItem mItems[];
    private OnItemClicked listener;

    public IndexBookAdapter(Context mContext, OutlineItem[] mItems,OnItemClicked  listener) {
        this.mContext = mContext;
        this.mItems = mItems;
        this.listener = listener;
    }

    @Override
    public IndexBookAdapter.IndexViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.custom_list_index, parent, false);
        IndexViewHolder holder = new IndexViewHolder(v);

        return holder;
    }

    @Override
    public void onBindViewHolder(IndexBookAdapter.IndexViewHolder holder, final int position) {

        holder.indexName.setText(""+mItems[position].title);
        holder.indexPage.setText(String.valueOf(mItems[position].page + 1));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(v,position,mItems[position].page);
            }
        });
//        int level = mItems[position].level;
//        if (level > 8) level = 8;
//        String space = "";
//        for (int i = 0; i < level; i++) {
//            space += "  ";
//            holder.indexName.setText(space + mItems[position].title);
//            holder.indexPage.setText(String.valueOf(mItems[position].page + 1));
//        }
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }



    public static class IndexViewHolder extends RecyclerView.ViewHolder {

        public CardView cardView;
        public TextView indexName;
        public TextView indexPage;

        public IndexViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cardView_index);
            indexName = (TextView) itemView.findViewById(R.id.tv_index_name);
            indexPage = (TextView) itemView.findViewById(R.id.tv_index_page);
        }
    }

    public interface OnItemClicked {
        void onItemClick(View view, int position,int page);
    }
}
