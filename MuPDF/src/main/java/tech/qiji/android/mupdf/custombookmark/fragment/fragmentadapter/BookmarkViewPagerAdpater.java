package tech.qiji.android.mupdf.custombookmark.fragment.fragmentadapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import tech.qiji.android.mupdf.custombookmark.fragment.FragmentBookmark;
import tech.qiji.android.mupdf.custombookmark.fragment.FragmentPreviewPage;


/**
 * Created by Changefouk on 15/9/2560.
 */

public class BookmarkViewPagerAdpater extends android.support.v4.app.FragmentPagerAdapter {

    private Context mContext;


    private String mBookid;
    private String mBookpage;
    private String[] tabTitles;

    public BookmarkViewPagerAdpater(Context context, FragmentManager fm, String[] tabTitles, String bookid, String bookPage) {
        super(fm);
        mContext = context;
        mBookid = bookid;
        mBookpage = bookPage;
        this.tabTitles = tabTitles;


    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return FragmentPreviewPage.newInstance(mBookid);
        } else if (position ==1){
            return  FragmentBookmark.newInstance(mBookid,mBookpage);
        }
        return null;
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
