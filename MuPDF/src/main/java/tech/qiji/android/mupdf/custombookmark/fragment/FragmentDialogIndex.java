package tech.qiji.android.mupdf.custombookmark.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import tech.qiji.android.mupdf.OutlineActivityData;
import tech.qiji.android.mupdf.OutlineItem;
import tech.qiji.android.mupdf.R;
import tech.qiji.android.mupdf.custombookmark.adapter.IndexBookAdapter;


/**
 * Created by Changefouk on 15/9/2560.
 */

public class FragmentDialogIndex extends DialogFragment {

    private OutlineItem mItems[];
    private RecyclerView recyclerView;
    private IndexBookAdapter indexBookAdapter;

    public FragmentDialogIndex(){
        super();
    }

    public static FragmentDialogIndex newInstance() {

        Bundle args = new Bundle();
        FragmentDialogIndex fragment = new FragmentDialogIndex();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.custom_dialog_fragment, container);
        initInstance(rootView);
        return rootView;
    }

    private void initInstance(View rootView) {

        mItems = OutlineActivityData.get().items;

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView_Index);
        recyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext()));



        this.getDialog().setTitle(getString(R.string.Index_lang));

    }
}
