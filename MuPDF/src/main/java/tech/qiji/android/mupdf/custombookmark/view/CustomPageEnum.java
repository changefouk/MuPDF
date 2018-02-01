package tech.qiji.android.mupdf.custombookmark.view;


import tech.qiji.android.mupdf.R;

/**
 * Created by Changefouk on 3/10/2560.
 */

public enum CustomPageEnum {

    PAGE1(R.string.PAGE1, R.layout.viewads1),
    PAGE2(R.string.PAGE2, R.layout.viewads2),
    PAGE3(R.string.PAGE3, R.layout.viewads3);

    private int mTitleResId;
    private int mLayoutResId;

    CustomPageEnum(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }
}
