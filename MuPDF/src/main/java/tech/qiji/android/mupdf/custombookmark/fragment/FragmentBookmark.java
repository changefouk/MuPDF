package tech.qiji.android.mupdf.custombookmark.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tech.qiji.android.mupdf.MuPDFCore;
import tech.qiji.android.mupdf.R;
import tech.qiji.android.mupdf.custombookmark.activity.Bookmark;
import tech.qiji.android.mupdf.custombookmark.adapter.BookMarkAdpater;
import tech.qiji.android.mupdf.custombookmark.model.BookmarkModel;


/**
 * Created by Changefouk on 15/9/2560.
 */

public class FragmentBookmark extends Fragment {

    final String BOOKMARK_DESCRIPTION = "PAGEDESCRIP";
    final String BOOKMARK_PAGE_NUMBER = "PAGES";
    final String TAG = "FragmentBookmark";

    MuPDFCore mCore;

    private String bookId;
    private String pageNumber;
    private ImageButton addBookMark;

    private EditText editPagebookmark;
    private EditText editDescriptionBook;

    private RecyclerView mRecyclerView;

    List<BookmarkModel> bookModels;

    public FragmentBookmark() {
    }

    public static FragmentBookmark newInstance(String bookId, String pageNumber) {

        FragmentBookmark fragment = new FragmentBookmark();
        Bundle args = new Bundle();
        args.putString("BOOK_ID", bookId);
        args.putString("NUMBER_PAGE", pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookId = getArguments().getString("BOOK_ID");
            pageNumber = getArguments().getString("NUMBER_PAGE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_bookmark, container, false);

        mCore = ((Bookmark) getActivity()).getCore();

        initInstance(rootView);


        return rootView;
    }

    private void initInstance(View rootView) {

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView_Bookmark);
//        mRecyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        mRecyclerView.setHasFixedSize(true);

        GridLayoutManager mLayoutManager;
        int orientation = getResources().getConfiguration().orientation;
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE && screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            mLayoutManager = new GridLayoutManager(getActivity(), 4);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mLayoutManager = new GridLayoutManager(getActivity(), 3);
        }else {
            mLayoutManager = new GridLayoutManager(getActivity(), 2);
        }

        mRecyclerView.setLayoutManager(mLayoutManager);

        SharedPreferences prefdataBookMark = getActivity().getSharedPreferences(bookId, Context.MODE_PRIVATE);
        SharedPreferences prefdataBookMark_boolean = getActivity().getSharedPreferences(bookId + "_boolean", Context.MODE_PRIVATE);

        Map<String, ?> keys = prefdataBookMark.getAll();
        Map<String, ?> keys_boolean = prefdataBookMark_boolean.getAll();

        List<BookmarkModel> listBookMarkModels = new ArrayList<>();
        Log.d("map values", "key size" + keys.size());

        List<Integer> bookMarkPage = new ArrayList<>();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());

            BookmarkModel bookmarkModel = new BookmarkModel();
            bookmarkModel.setPage(Integer.parseInt(entry.getKey()));
            bookmarkModel.setDescription(entry.getValue().toString());
            listBookMarkModels.add(bookmarkModel);

        }

        for (Map.Entry<String, ?> entry_boolean : keys_boolean.entrySet()) {
            Log.d("map values boolean", entry_boolean.getKey() + ": " + entry_boolean.getValue().toString());

            for (int i = 0; i < listBookMarkModels.size(); i++) {
                if (String.valueOf(listBookMarkModels.get(i).getPage() + "_boolean").equals(entry_boolean.getKey())) {
                    Log.d(TAG, "setbookmark" + listBookMarkModels.get(i).getPage());
                    listBookMarkModels.get(i).setBookmark(true);
                }
            }
//
//        Collections.sort(bookMarkPage);
//        Log.d("BOOKMARK",""+bookMarkPage.size());
//        for (int i = 0;i<bookMarkPage.size();i++){
//            BookmarkModel bookmodel = new BookmarkModel();
//            bookmodel.setPage();
//            bookmodel.setDescription(bookMarkPage.get(i));
//            bookmarkModels.add(bookmodel);
//        }

            BookMarkAdpater bookMarkAdpater = new BookMarkAdpater(bookId, getActivity(), listBookMarkModels, mCore);
            mRecyclerView.setAdapter(bookMarkAdpater);

        }

    }

    private void addbookMark(View v) {

        SharedPreferences prefsBookmark = getActivity().getSharedPreferences(bookId, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefsBookmark.edit();

        int page = Integer.parseInt(pageNumber);
        edit.putString(String.valueOf(page), editDescriptionBook.getText().toString());
        edit.commit();

        SharedPreferences prefsBookmark_boolean = getActivity().getSharedPreferences(bookId + "_boolean", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit_boolean = prefsBookmark_boolean.edit();
        edit_boolean.putBoolean(String.valueOf(page) + "_boolean", true);
        edit_boolean.commit();
    }
}
