package tech.qiji.android.mupdf.custombookmark.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * Created by Changefouk on 4/10/2560.
 */

public class BookMarkAdpater extends RecyclerView.Adapter<BookMarkAdpater.BookmarkViewHolder> {

    private static final String TAG = "BOOKMARKADPATER";

    private MuPDFCore core;

    private String bookId;
    private Context mContext;
    private List<BookmarkModel> bookModels;

    int currentlyViewing;
    private File cacheDir;

    public BookMarkAdpater(String bookId, Context mContext, List<BookmarkModel> bookModels, MuPDFCore core) {
        this.bookId = bookId;
        this.mContext = mContext;
        this.bookModels = bookModels;
        this.core = core;
    }

    @Override
    public BookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.bookmark_page, parent, false);
        final BookmarkViewHolder bookmarkViewHolder = new BookmarkViewHolder(v);

        bookmarkViewHolder.imageBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int viewposition = bookmarkViewHolder.getLayoutPosition();
                int position = bookModels.get(viewposition).getPage() - 1;
                Intent intent = new Intent();
                intent.putExtra("PAGE_RESULT", position);
                Activity activity = (Activity) mContext;
                activity.setResult(Activity.RESULT_OK, intent);
                activity.finish();
            }
        });

        return bookmarkViewHolder;
    }

    @Override
    public void onBindViewHolder(final BookmarkViewHolder holder, final int position) {
        holder.bookmarkPage.setText("" + bookModels.get(position).getPage());
        // holder.descriptionBookMark.setText("Description : " + bookModels.get(position).getDescription());

        if (bookModels.get(position).getDescription().isEmpty()) {
            holder.infoButton.setImageResource(R.drawable.note);
        } else {
            Log.d(TAG, "Have descrip : " + bookModels.get(position).getPage());
            holder.infoButton.setImageResource(R.drawable.note_1);
        }

        holder.infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialogDetailBookmark(position);

            }
        });
        drawPreview(holder.imageBookmark, bookModels.get(position).getPage() - 1);
    }

    private void dialogDetailBookmark(final int position) {

        final Dialog dialog = new Dialog(mContext, R.style.FullHeightDialog);
        //dialog.setTitle(R.string.bookmark_detail);
        dialog.setContentView(R.layout.custom_dialog_detail_bookmark);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView pageNumber = (TextView) dialog.findViewById(R.id.tv_bookmark_page_dialog);
        final EditText pageDescription = (EditText) dialog.findViewById(R.id.edit_bookmark_description_dialog);
        pageDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (null != pageDescription.getLayout() && pageDescription.getLayout().getLineCount() > 2) {
                    pageDescription.getText().delete(pageDescription.getText().length() - 1, pageDescription.getText().length());
                }
            }
        });
        pageDescription.setSelection(pageDescription.getText().length());
        Button cancelButton = (Button) dialog.findViewById(R.id.button_cancel_dialog);
        Button saveButton = (Button) dialog.findViewById(R.id.button_save_dialog);

        pageNumber.setText("" + bookModels.get(position).getPage());
        pageDescription.setText(bookModels.get(position).getDescription());

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefsBookmark = mContext.getSharedPreferences(bookId, Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefsBookmark.edit();

                int page = bookModels.get(position).getPage();

                edit.putString(String.valueOf(page), pageDescription.getText().toString());
                edit.commit();

                SharedPreferences prefsBookmark_boolean = mContext.getSharedPreferences(bookId + "_boolean", Context.MODE_PRIVATE);
                SharedPreferences.Editor edit_boolean = prefsBookmark_boolean.edit();
                edit_boolean.putBoolean(String.valueOf(page) + "_boolean", true);
                edit_boolean.commit();

                bookModels.get(position).setDescription(prefsBookmark.getString(String.valueOf(page), ""));
                Log.d(TAG, "Description change : " + bookModels.get(position).getDescription());

                notifyDataSetChanged();

                Toast.makeText(mContext.getApplicationContext(), R.string.savecomplete, Toast.LENGTH_SHORT).show();

                dialog.dismiss();

            }
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return bookModels.size();
    }

    public static class BookmarkViewHolder extends RecyclerView.ViewHolder {

        public CardView cardView;
        public TextView bookmarkPage;
        public TextView descriptionBookMark;
        public ImageView imageBookmark;
        public ImageButton infoButton;

        public BookmarkViewHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.card_view_bookmark);
            bookmarkPage = (TextView) itemView.findViewById(R.id.tv_Bookmark_page);
            descriptionBookMark = (TextView) itemView.findViewById(R.id.tv_Description_Bookmark);
            imageBookmark = (ImageView) itemView.findViewById(R.id.image_page_bookmark);
            infoButton = (ImageButton) itemView.findViewById(R.id.info_description_bookmark_button);

        }
    }


    public int getCurrentlyViewing() {
        return currentlyViewing;
    }

    public void setCurrentlyViewing(int currentlyViewing) {
        this.currentlyViewing = currentlyViewing;
        notifyDataSetChanged();
    }

    public void drawPreview(ImageView imageView, int position) {
        if (cancelPotentialWork(imageView, position)) {
            Bitmap mLoadingBitmap = BitmapFactory.decodeResource(
                    //TODO : Image Loading
                    mContext.getResources(), R.drawable.bookloading);
            final BookMarkAdpater.BitmapTask task = new BookMarkAdpater.BitmapTask(position, imageView);
            final BookMarkAdpater.AsyncDrawable asyncDrawable = new BookMarkAdpater.AsyncDrawable(
                    mContext.getResources(), mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute();
        }
    }

    public static boolean cancelPotentialWork(ImageView imageView, int position) {
        final BookMarkAdpater.BitmapTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

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

    private static BookMarkAdpater.BitmapTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof BookMarkAdpater.AsyncDrawable) {
                final BookMarkAdpater.AsyncDrawable asyncDrawable = (BookMarkAdpater.AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BookMarkAdpater.BitmapTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapTask>(
                    bitmapWorkerTask);
        }

        public BookMarkAdpater.BitmapTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }


    public class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
        private int position;
        ImageView preView;
        LinearLayout preViewLinear;

        public BitmapTask(int position, ImageView preView) {
            this.position = position;
            this.preView = preView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            Bitmap bitmap = getBitmapCached(position);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {

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

    private Bitmap getBitmapCached(final int position) {

        String cacheName = bookId + "_" + position + ".jpg";
        cacheDir = new CacheDirectoryHelper(mContext).getCacheDir(bookId);

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

        File imageCach = new File(cacheDir + "/" + cacheName);
        Log.w("imageCache", "  " + imageCach);
        try {
            if (imageCach.exists() && imageCach.canRead()) {
                Log.d(TAG, "page " + position + " found in cache");
                bm = BitmapFactory.decodeFile(imageCach.getAbsolutePath());
                return bm;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // some error with cached file,
            // delete the file and get rid of bitmap
            if (imageCach.exists())
                imageCach.delete();

            bm = null;
        }

        if (bm == null) {
            bm = Bitmap.createBitmap(160, 220, Bitmap.Config.ARGB_8888);
            core.drawPage(bm, position, 160, 220, 0, 0, 160, 220, core.new Cookie());

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
