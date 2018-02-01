package tech.qiji.android.mupdf.custombookmark.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import tech.qiji.android.mupdf.MuPDFCore;
import tech.qiji.android.mupdf.R;
import tech.qiji.android.mupdf.custombookmark.activity.Bookmark;
import tech.qiji.android.mupdf.custombookmark.adapter.PreviewPageAdapter;
import tech.qiji.android.mupdf.custombookmark.model.BookmarkModel;


/**
 * Created by Changefouk on 15/9/2560.
 */

public class FragmentPreviewPage extends Fragment {

    final String TAG = "PreviewPage";
    MuPDFCore core;
    String bookID;

    public FragmentPreviewPage(){

    }

    public static FragmentPreviewPage newInstance(String bookID) {
        FragmentPreviewPage fragment = new FragmentPreviewPage();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        args.putString("BOOK_ID",bookID);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookID = getArguments().getString("BOOK_ID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_previewpage, container, false);
        initInstance(rootView);
        return rootView;
    }

    private void initInstance(View rootView) {
        core = ((Bookmark) getActivity()).getCore();

        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView_Preview);
        mRecyclerView.setHasFixedSize(true);

        GridLayoutManager mLayoutManager;
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mLayoutManager = new GridLayoutManager(getActivity(), 3);
        } else {
            mLayoutManager = new GridLayoutManager(getActivity(), 2);
        }

        mRecyclerView.setLayoutManager(mLayoutManager);

        SharedPreferences prefsBookmark = getActivity().getSharedPreferences(bookID, Context.MODE_PRIVATE);

        List<BookmarkModel> bookModels =new ArrayList<>();
        for (int i = 0;i<core.countPages();i++){
            BookmarkModel bookModel = new BookmarkModel();
            bookModel.setPage(i);
//            boolean isBookmark = prefsBookmark.getBoolean(String.valueOf(i), false);
//            bookModel.setBookmark(isBookmark);

            bookModels.add(bookModel);
        }

        PreviewPageAdapter adapter = new PreviewPageAdapter(bookID,bookModels,getActivity(),core);
        mRecyclerView.setAdapter(adapter);
    }

}
