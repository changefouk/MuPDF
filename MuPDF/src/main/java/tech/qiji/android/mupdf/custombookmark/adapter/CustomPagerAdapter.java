package tech.qiji.android.mupdf.custombookmark.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.bumptech.glide.Glide;

import tech.qiji.android.mupdf.R;
import tech.qiji.android.mupdf.custombookmark.view.CustomPageEnum;

/**
 * Created by Changefouk on 3/10/2560.
 */

public class CustomPagerAdapter extends PagerAdapter {

    private Context mContext;
    private String url1,url2,url3;

    public CustomPagerAdapter(Context context, String Url1, String Url2, String Url3) {
        mContext = context;
        this.url1 = Url1;
        this.url2 = Url2;
        this.url3 = Url3;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        CustomPageEnum customPageEnum = CustomPageEnum.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(customPageEnum.getLayoutResId(), collection, false);

        final int mPosition = position;
        ImageView imageViewAds = (ImageView) layout.findViewById(R.id.imageAds);

        if (position ==0){
            Glide.with(mContext).load(url1).into(imageViewAds);
        }else if (position == 1){
            Glide.with(mContext).load(url2).into(imageViewAds);
        }else if (position == 2){
            Glide.with(mContext).load(url3).into(imageViewAds);
        }


        imageViewAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView i = (ImageView) v;
                if (mPosition == 0){
                    Log.d("PagerAdapter","you Click position "+mPosition);

                    String url = "http://www.google.com";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    mContext.startActivity(intent);
                }
                else if (mPosition == 1){
                    Log.d("PagerAdapter","you Click position "+mPosition);

                    String url = "http://www.facebook.com";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    mContext.startActivity(intent);
                }
                else if (mPosition == 2){
                    Log.d("PagerAdapter","you Click position "+mPosition);

                    String url = "http://www.google.com";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    mContext.startActivity(intent);
                }
            }
        });

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return CustomPageEnum.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        CustomPageEnum customPagerEnum = CustomPageEnum.values()[position];
        return mContext.getString(customPagerEnum.getTitleResId());
    }
}
