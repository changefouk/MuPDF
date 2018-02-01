package tech.qiji.android.mupdf.custombookmark.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import tech.qiji.android.mupdf.AsyncTask;
import tech.qiji.android.mupdf.MuPDFCore;
import tech.qiji.android.mupdf.R;
import tech.qiji.android.mupdf.custombookmark.manager.CacheDirectoryHelper;
import tech.qiji.android.mupdf.custombookmark.model.BookmarkModel;


/**
 * Created by Changefouk on 10/10/2016
 */
public class PreviewPageAdapter extends RecyclerView.Adapter<PreviewPageAdapter.PreviewHolder> {
    private final String TAG ="PreviewPageAdapter";
    private String bookId;
    private MuPDFCore core;
    private Context context;
    private List<BookmarkModel> bookModels;
    private File cacheDir;
    int currentlyViewing;


    public PreviewPageAdapter(String bookId, List<BookmarkModel> bookModels, Context context, MuPDFCore core) {
        this.bookId = bookId;
        this.bookModels = bookModels;
        this.context = context;
        this.core = core;

    }

    @Override
    public PreviewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.custom_grid_preview, null, false);

        final PreviewHolder previewHolder =new PreviewHolder(view);

        previewHolder.previewLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int viewposition = previewHolder.getLayoutPosition();
                int position = bookModels.get(viewposition).getPage();
                Intent intent = new Intent();
                intent.putExtra("PAGE_RESULT",position);
                Activity activity = (Activity) context;
                activity.setResult(Activity.RESULT_OK,intent);
                activity.finish();
            }
        });

        return previewHolder;
    }

    @Override
    public void onBindViewHolder(PreviewHolder holder, int position) {

        holder.pageNum.setText(String.valueOf(bookModels.get(position).getPage()+1));
//        if (bookModels.get(position).isBookmark())
//            holder.bookmarked.setVisibility(View.VISIBLE);
//        else
//            holder.bookmarked.setVisibility(View.INVISIBLE);


        drawPreview(holder.previewPage,position,holder.previewLinear);



    }

    @Override
    public int getItemCount() {
        return bookModels.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public static class PreviewHolder extends RecyclerView.ViewHolder {

        public ImageView previewPage;
        public ImageView bookmarked;
        public TextView pageNum;
        public LinearLayout previewLinear;
        public PreviewHolder(View itemView) {
            super(itemView);

            previewLinear = (LinearLayout) itemView.findViewById(R.id.preview_linear);
            pageNum       = (TextView) itemView.findViewById(R.id.page_num_preview);
            previewPage   = (ImageView) itemView.findViewById(R.id.image_page_preview);
//            bookmarked    = (ImageView) itemView.findViewById(R.id.bookmarked);

        }
    }



    public int getCurrentlyViewing() {
        return currentlyViewing;
    }

    public void setCurrentlyViewing(int currentlyViewing) {
        this.currentlyViewing = currentlyViewing;
        notifyDataSetChanged();
    }





    public void drawPreview(ImageView imageView, int position, LinearLayout previewLinear){
        if (cancelPotentialWork(imageView, position)) {
            Bitmap mLoadingBitmap = BitmapFactory.decodeResource(
                    //TODO : Image Loading
                    context.getResources(), R.drawable.bookloading);
            final BitmapTask task = new BitmapTask(position, imageView,previewLinear);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(
                    context.getResources(), mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute();
        }
    }

    public static boolean cancelPotentialWork(ImageView imageView, int position) {
        final BitmapTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int bitmapPosition = bitmapWorkerTask.position;
            if (bitmapPosition != position) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was
        // cancelled
        return true;
    }

    private static BitmapTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapTask>(
                    bitmapWorkerTask);
        }

        public BitmapTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }


    public class BitmapTask extends AsyncTask<Void,Void,Bitmap> {
        private int position;
        ImageView preView;
        LinearLayout preViewLinear;

        public BitmapTask(int position, ImageView preView, LinearLayout preViewLinear){
            this.position=position;
            this.preView=preView;
            this.preViewLinear = preViewLinear;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            Bitmap bitmap = getBitmapCached(position);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if(bitmap!=null){

                preView.setImageBitmap(bitmap);
            }



        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

//            if(position==getCurrentlyViewing()){
//                preViewLinear.setBackgroundColor(context.getResources().getColor(R.color.button_pressed));
//            }
//            else {
//                preViewLinear.setBackgroundColor(Color.TRANSPARENT);
//            }
        }
    }

    private Bitmap getBitmapCached(final int position){

            String cacheName = bookId+"_"+position+".jpg";
        cacheDir = new CacheDirectoryHelper(context).getCacheDir(bookId);

        Log.d("Cach DIR", cacheDir.toString());


        Bitmap bm = null;
        if (!cacheDir.exists()) {
            Log.i(TAG, "cachDir Not Exsit");
            try {
                boolean mk = cacheDir.mkdirs();
                Log.e(TAG, mk + "Make");
            } catch (Exception e) {
                Log.e(TAG, e.toString());

            }

        }

        File imageCach = new File(cacheDir+"/"+cacheName);
        Log.w("imageCache","  "+imageCach);
        try {
            if (imageCach.exists() && imageCach.canRead()) {
                Log.d(TAG, "page " + position + " found in cache");
                bm = BitmapFactory.decodeFile(imageCach.getAbsolutePath());
                return bm;
            }
        }catch (Exception e){
            e.printStackTrace();
            // some error with cached file,
            // delete the file and get rid of bitmap
            if(imageCach.exists())
                imageCach.delete();

            bm = null;
        }

        if(bm==null){
            bm = Bitmap.createBitmap(160, 220, Bitmap.Config.ARGB_8888);
            core.drawPage(bm,position,160,220,0,0,160,220,core.new Cookie());

            try {
                bm.compress(Bitmap.CompressFormat.JPEG, 50, new FileOutputStream(
                        imageCach));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                imageCach.delete();
            }
        }


        return bm;

    }
}
