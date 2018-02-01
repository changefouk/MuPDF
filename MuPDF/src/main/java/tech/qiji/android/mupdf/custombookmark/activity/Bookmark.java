package tech.qiji.android.mupdf.custombookmark.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;


import com.astuetz.PagerSlidingTabStrip;

import java.io.InputStream;

import tech.qiji.android.mupdf.MuPDFCore;
import tech.qiji.android.mupdf.OutlineActivityData;
import tech.qiji.android.mupdf.R;
import tech.qiji.android.mupdf.SearchTaskResult;
import tech.qiji.android.mupdf.custombookmark.fragment.fragmentadapter.BookmarkViewPagerAdpater;

public class Bookmark extends ActionBarActivity {

    private static final String TAG = "BOOKMARKACTIVITY";

    private ViewPager viewPager;

    private MuPDFCore core;

    private String bookId;
    private String pageNumber;
    private String pathFile;
    private String mFileName;
    private String mPassword;
    private Uri uri;

    private AlertDialog.Builder mAlertBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppBaseTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        bookId = getIntent().getStringExtra("BOOK_ID");
        pageNumber = getIntent().getStringExtra("NUMBER_PAGE");
        pathFile = getIntent().getStringExtra("URI_PATH");
        mFileName = getIntent().getStringExtra("BOOK_NAME");
        mPassword = getIntent().getStringExtra("BOOK_PASSWORD");

//                if (savedInstanceState == null){
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.content_bookmark, FragmentBookmark.newInstance(bookId,pageNumber))
//                    .commit();
//        }
        initInstance();
        openCore(savedInstanceState);

    }


    private void initInstance() {

        String pageTitle[] = getResources().getStringArray(R.array.bookmark_detail_pager);
        viewPager = (ViewPager) findViewById(R.id.viewpager_bookmark);
        BookmarkViewPagerAdpater bookmarkViewPagerAdpater = new BookmarkViewPagerAdpater(this, getSupportFragmentManager(), pageTitle, bookId, pageNumber);
        viewPager.setAdapter(bookmarkViewPagerAdpater);

        PagerSlidingTabStrip tab = (PagerSlidingTabStrip) findViewById(R.id.tab_sliding);
        tab.setViewPager(viewPager);

    }

    private void openCore(Bundle savedInstanceState) {

        uri = Uri.parse(pathFile);

        mAlertBuilder = new AlertDialog.Builder(this);

        if (core == null) {
            core = (MuPDFCore) getLastNonConfigurationInstance();

            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
                pathFile = savedInstanceState.getString("URI_PATH");
                mFileName = savedInstanceState.getString("BOOK_NAME");
                mPassword = savedInstanceState.getString("BOOK_PASSWORD");
            }
        }
        if (core == null) {
            Intent intent = getIntent();
            byte buffer[] = null;
            if (uri.toString().length() > 0) {

                if (uri.toString().startsWith("content://")) {
                    String reason = null;
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        int len = is.available();
                        buffer = new byte[len];
                        is.read(buffer, 0, len);
                        is.close();
                    } catch (java.lang.OutOfMemoryError e) {
                        System.out.println("Out of memory during buffer reading");
                        reason = e.toString();
                    } catch (Exception e) {
                        System.out.println("Exception reading from stream: " + e);

                        // Handle view requests from the Transformer Prime's file manager
                        // Hopefully other file managers will use this same scheme, if not
                        // using explicit paths.
                        // I'm hoping that this case below is no longer needed...but it's
                        // hard to test as the file manager seems to have changed in 4.x.
                        try {
                            Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                            if (cursor.moveToFirst()) {
                                String str = cursor.getString(0);
                                if (str == null) {
                                    reason = "Couldn't parse data in intent";
                                } else {
                                    uri = Uri.parse(str);
                                }
                            }
                        } catch (Exception e2) {
                            System.out.println("Exception in Transformer Prime file manager code: " + e2);
                            reason = e2.toString();
                        }
                    }
                    if (reason != null) {
                        buffer = null;
                        Resources res = getResources();
                        AlertDialog alert = mAlertBuilder.create();
                        setTitle(String.format(res.getString(R.string.cannot_open_document_Reason), reason));
                        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                        alert.show();
                        return;
                    }
                }
                if (buffer != null) {
                    core = openBuffer(buffer, intent.getType());
                } else {
                    String path = Uri.decode(uri.getEncodedPath());
                    if (path == null) {
                        path = uri.toString();
                    }
                    core = openFile(path);
                }
                SearchTaskResult.set(null);
            }
            if (core != null && core.needsPassword()) {
//				requestPassword(savedInstanceState);
//				return;
                if (core.authenticatePassword(mPassword)) {
                    return;
                } else {
                    Log.d(TAG, "don't have password ask for password");
                }
                return;
            }
            if (core != null && core.countPages() == 0) {
                core = null;
            }
        }


        if (core == null) {
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.cannot_open_document);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alert.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            alert.show();
            return;
        }

    }

//    private MuPDFCore openFile(String path) {
//        /*int lastSlashPos = path.lastIndexOf('/');
//		mFileName = new String(lastSlashPos == -1
//					? path
//					: path.substring(lastSlashPos+1));*/
//        System.out.println("Trying to open " + path);
//        try {
//            core = new MuPDFCore(this, path);
//            // New file: drop the old outline data
//            OutlineActivityData.set(null);
//        } catch (Exception e) {
//            System.out.println(e);
//            return null;
//        }
//        return core;
//    }


    private MuPDFCore openFile(String path) {
        int lastSlashPos = path.lastIndexOf('/');
        mFileName = new String(lastSlashPos == -1 ? path :
                path.substring(lastSlashPos + 1));
        System.out.println("Trying to open " + path);
        try {
            core = new MuPDFCore(this, path);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        } catch (OutOfMemoryError e) {
            //  out of memory is not an Exception, so we catch it separately.
            System.out.println(e);
            return null;
        }
        return core;
    }

    private MuPDFCore openBuffer(byte buffer[], String magic) {
        System.out.println("Trying to open byte buffer");
        try {
            core = new MuPDFCore(this, buffer, magic);
            // New file: drop the old outline data
            OutlineActivityData.set(null);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return core;
    }

    public MuPDFCore getCore() {
        return core;
    }

}
