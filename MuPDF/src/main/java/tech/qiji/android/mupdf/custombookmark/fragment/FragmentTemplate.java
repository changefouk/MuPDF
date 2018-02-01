package tech.qiji.android.mupdf.custombookmark.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import tech.qiji.android.mupdf.R;


/**
 * Created by Changefouk on 15/9/2560.
 */

public class FragmentTemplate extends Fragment {

    public FragmentTemplate(){
        super();
    }

    public static FragmentTemplate newInstance() {

        Bundle args = new Bundle();

        FragmentTemplate fragment = new FragmentTemplate();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_bookmark, container, false);
        initInstance(rootView);
        return rootView;
    }

    private void initInstance(View rootView) {

    }
}
