package tech.qiji.android.mupdf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.google.gson.Gson;
import com.rd.PageIndicatorView;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import tech.qiji.android.mupdf.custombookmark.activity.Bookmark;
import tech.qiji.android.mupdf.custombookmark.adapter.CustomPagerAdapter;
import tech.qiji.android.mupdf.custombookmark.adapter.IndexBookAdapter;
import tech.qiji.android.mupdf.custombookmark.manager.CacheDirectoryHelper;
import tech.qiji.android.mupdf.customeditor.model.EditorModel;

class ThreadPerTaskExecutor implements Executor {
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}

public class MuPDFActivity extends Activity implements FilePicker.FilePickerSupport {
    /* The core rendering instance */
    enum TopBarMode {
        Main, Search, Annot, Delete, More, Accept
    }

    enum AcceptMode {Highlight, Underline, StrikeOut, Ink, CopyText}

    final String TAG = "MUpdfActivity";

    private final int OUTLINE_REQUEST = 0;
    private final int PRINT_REQUEST = 1;
    private final int FILEPICK_REQUEST = 2;
    private final int PROOF_REQUEST = 3;
    private final int BOOKMARK_RESULT = 4;

    private MuPDFCore core;
    private String mFileName;
    private MuPDFReaderView mDocView;
    private View mButtonsView;
    private boolean mButtonsVisible;
    private EditText mPasswordView;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private TextView mInfoView;
    private ImageButton mSearchButton;
    private ImageButton mReflowButton;
    private ImageButton mOutlineButton;
    private ImageButton mMoreButton;
    private TextView mAnnotTypeText;
    private ImageButton mAnnotButton;
    private ViewAnimator mTopBarSwitcher;
    private ImageButton mLinkButton;
    private TopBarMode mTopBarMode = TopBarMode.Main;
    private AcceptMode mAcceptMode;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private EditText mSearchText;
    private SearchTask mSearchTask;
    private ImageButton mProofButton;
    private ImageButton mSepsButton;
    private AlertDialog.Builder mAlertBuilder;
    private boolean mLinkHighlight = false;
    private final Handler mHandler = new Handler();
    private boolean mAlertsActive = false;
    private boolean mReflow = false;
    private AsyncTask<Void, Void, MuPDFAlert> mAlertTask;
    private AlertDialog mAlertDialog;
    private FilePicker mFilePicker;
    private String mProofFile;
    private boolean mSepEnabled[][];

    static private AlertDialog.Builder gAlertBuilder;

    static public AlertDialog.Builder getAlertBuilder() {
        return gAlertBuilder;
    }

    // TODO : set con
    private View AdsView;
    private ViewPager viewPagerAds;
    private PageIndicatorView pageIndicatorView;

    private ImageButton mBookmarkButton;
    private ImageButton mBookmarkDeTailButton;

    private String mPassword;
    private String mSpritPassword = "";
    private String mBookId;
    private String mBookName;
    private Uri mPathFile;

    private RelativeLayout thumnailview;
    private ImageView previewSeek;

    private RecyclerView indexRecyclerview;
    private IndexBookAdapter indexBookAdapter;
    private Button okButtonDialog;

    public void createAlertWaiter() {
        mAlertsActive = true;
        // All mupdf library calls are performed on asynchronous tasks to avoid stalling
        // the UI. Some calls can lead to javascript-invoked requests to display an
        // alert dialog and collect a reply from the user. The task has to be blocked
        // until the user's reply is received. This method creates an asynchronous task,
        // the purpose of which is to wait of these requests and produce the dialog
        // in response, while leaving the core blocked. When the dialog receives the
        // user's response, it is sent to the core via replyToAlert, unblocking it.
        // Another alert-waiting task is then created to pick up the next alert.
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mAlertTask = new AsyncTask<Void, Void, MuPDFAlert>() {

            @Override
            protected MuPDFAlert doInBackground(Void... arg0) {
                if (!mAlertsActive) return null;

                return core.waitForAlert();
            }

            @Override
            protected void onPostExecute(final MuPDFAlert result) {
                // core.waitForAlert may return null when shutting down
                if (result == null) return;
                final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
                for (int i = 0; i < 3; i++)
                    pressed[i] = MuPDFAlert.ButtonPressed.None;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            int index = 0;
                            switch (which) {
                                case AlertDialog.BUTTON1:
                                    index = 0;
                                    break;
                                case AlertDialog.BUTTON2:
                                    index = 1;
                                    break;
                                case AlertDialog.BUTTON3:
                                    index = 2;
                                    break;
                            }
                            result.buttonPressed = pressed[index];
                            // Send the user's response to the core, so that it can
                            // continue processing.
                            core.replyToAlert(result);
                            // Create another alert-waiter to pick up the next alert.
                            createAlertWaiter();
                        }
                    }
                };
                mAlertDialog = mAlertBuilder.create();
                mAlertDialog.setTitle(result.title);
                mAlertDialog.setMessage(result.message);
                switch (result.iconType) {
                    case Error:
                        break;
                    case Warning:
                        break;
                    case Question:
                        break;
                    case Status:
                        break;
                }
                switch (result.buttonGroupType) {
                    case OkCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.cancel), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
                    case Ok:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.okay), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Ok;
                        break;
                    case YesNoCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON3, getString(R.string.cancel), listener);
                        pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
                    case YesNo:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.yes), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Yes;
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.no), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.No;
                        break;
                }
                mAlertDialog.setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            result.buttonPressed = MuPDFAlert.ButtonPressed.None;
                            core.replyToAlert(result);
                            createAlertWaiter();
                        }
                    }
                });

                mAlertDialog.show();
            }
        };

        mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
    }

    public void destroyAlertWaiter() {
        mAlertsActive = false;
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
    }

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

    //  determine whether the current activity is a proofing activity.
    public boolean isProofing() {
        String format = core.fileFormat();
        return (format.equals("GPROOF"));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlertBuilder = new AlertDialog.Builder(this);
        gAlertBuilder = mAlertBuilder;  //  keep a static copy of this that other classes can use

        //TODO : password pdf
        mBookId = getIntent().getStringExtra("BOOK_ID");
        mPassword = getIntent().getStringExtra("PASSWORD_PDF");
        mBookName = getIntent().getStringExtra("BOOK_NAME");

        if (core == null) {
            core = (MuPDFCore) getLastNonConfigurationInstance();

            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {

                mFileName = savedInstanceState.getString("mFilename");
                mPathFile = Uri.parse(savedInstanceState.getString("Pathfile"));
                mBookId = savedInstanceState.getString("BOOK_ID");
                mBookName = savedInstanceState.getString("BOOK_NAME");
                mSpritPassword = savedInstanceState.getString("BOOK_PASSWORD");
            }
        }
        if (core == null) {
            Intent intent = getIntent();
            byte buffer[] = null;

            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                System.out.println("URI to open is: " + uri);
                mPathFile = uri;
                mBookName = getIntent().getStringExtra("BOOK_NAME");
                //TODO : getnamefile generate password
                getPassWord();

                if (uri.toString().startsWith("content://")) {
                    String reason = null;
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        int len = is.available();
                        buffer = new byte[len];
                        is.read(buffer, 0, len);
                        is.close();
                    } catch (OutOfMemoryError e) {
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
                            Cursor cursor =
                                    getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
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
                if (core.authenticatePassword(mSpritPassword)) {
                    createUI(savedInstanceState);
                }
//                if (core.authenticatePassword(mPassword)) {
//                    createUI(savedInstanceState);}
                else {
                    Log.d(TAG, "don't have password ask for password");
                    requestPassword(savedInstanceState);
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
            alert.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            alert.show();
            return;
        }

        createUI(savedInstanceState);

        //  hide the proof button if this file can't be proofed
        if (!core.canProof()) {
            mProofButton.setVisibility(View.GONE);
        }

        if (isProofing()) {

            //  start the activity with a new array
            mSepEnabled = null;

            //  show the separations button
            mSepsButton.setVisibility(View.VISIBLE);

            //  hide some other buttons
            mLinkButton.setVisibility(View.INVISIBLE);
            mReflowButton.setVisibility(View.INVISIBLE);
            mOutlineButton.setVisibility(View.INVISIBLE);
            mSearchButton.setVisibility(View.INVISIBLE);
            mMoreButton.setVisibility(View.INVISIBLE);
        } else {
            //  hide the separations button
            mSepsButton.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        thumnailview.setVisibility(View.GONE);
        previewSeek.setVisibility(View.GONE);
    }

    private void getPassWord() {
        File getnameFile = new File("" + mPathFile);
        String filename = getnameFile.getName();
        String[] spritname = filename.split("-");

        for (int i = 0; i < spritname.length; i++) {
            if (i < 4) {
                String twochar = firstTwoLengthCharSprit(spritname[i]);
                String totalchar = mSpritPassword + twochar;
                mSpritPassword = totalchar;

                Log.d(TAG, "Password : " + mSpritPassword);
            }
        }
    }

    public String firstTwoLengthCharSprit(String str) {
        return str.length() < 2 ? str : str.substring(0, 2);
    }

    public void requestPassword(final Bundle savedInstanceState) {
        mPasswordView = new EditText(this);
        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(R.string.enter_password);
        alert.setView(mPasswordView);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (core.authenticatePassword(mPasswordView.getText().toString())) {
                            createUI(savedInstanceState);
                        } else {
                            requestPassword(savedInstanceState);
                        }
                    }
                });
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        alert.show();
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null) return;

        // Now create the UI.
        // First create the document view
        mDocView = new MuPDFReaderView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null) return;

                mPageNumberView.setText(String.format("%d / %d", i + 1, core.countPages()));
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);
                thumnailview.setVisibility(GONE);
                previewSeek.setVisibility(GONE);
                displayBookmarkButton();
                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    if (mTopBarMode == TopBarMode.Main) hideButtons();
                }
            }

            @Override
            protected void onDocMotion() {
                hideButtons();
            }

            @Override
            protected void onHit(Hit item) {
                switch (mTopBarMode) {
                    case Annot:
                        if (item == Hit.Annotation) {
                            showButtons();
                            mTopBarMode = TopBarMode.Delete;
                            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                        }
                        break;
                    case Delete:
                        mTopBarMode = TopBarMode.Annot;
                        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                        // fall through
                    default:
                        // Not in annotation editing mode, but the pageview will
                        // still select and highlight hit annotations, so
                        // deselect just in case.
                        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                        if (pageView != null) pageView.deselectAnnotation();
                        break;
                }
            }
        };
        mDocView.setAdapter(new MuPDFPageAdapter(this, this, core, loadEditor()));

        mSearchTask = new SearchTask(this, core) {
            @Override
            protected void onTextFound(SearchTaskResult result) {
                SearchTaskResult.set(result);
                // Ask the ReaderView to move to the resulting page
                mDocView.setDisplayedViewIndex(result.pageNumber);
                // Make the ReaderView act on the change to SearchTaskResult
                // via overridden onChildSetup method.
                mDocView.resetupChildren();
            }
        };

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;

        // Set the file-name text
        mFilenameView.setText(mBookName);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.setDisplayedViewIndex(
                        (seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
                thumnailview.setVisibility(View.GONE);
                previewSeek.setVisibility(View.GONE);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
                updatePositionThumnailSeekbar(seekBar, progress);
                Log.d(TAG, "countpage : " + core.countPages());
            }
        });

        // Activate the search-preparing button
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchModeOn();
            }
        });

        // Activate the reflow button
        mReflowButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleReflow();
            }
        });

        //TODO : editor pdf
        if (core.fileFormat().startsWith("PDF")
                && core.isUnencryptedPDF()
                && !core.wasOpenedFromBuffer()) {
            mAnnotButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mTopBarMode = TopBarMode.Annot;
                    mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                }
            });
        } else {
            mAnnotButton.setVisibility(View.GONE);
        }

        // Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        // React to interaction with the text widget
        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText()
                        .toString()
                        .equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        //React to Done button on keyboard
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) search(1);
                return false;
            }
        });

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    search(1);
                }
                return false;
            }
        });

        // Activate search invoking buttons
        mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(-1);
            }
        });
        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(1);
            }
        });

        mLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setLinkHighlight(!mLinkHighlight);
            }
        });

        mBookmarkDeTailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DetailBookmark();
            }
        });

        mBookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBookMark();
            }
        });

        if (core.hasOutline()) {
            mOutlineButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    OutlineItem outline[] = core.getOutline();
                    if (outline != null) {
                        OutlineActivityData.get().items = outline;
//                        Intent intent = new Intent(MuPDFActivity.this, OutlineActivity.class);
//                        startActivityForResult(intent, OUTLINE_REQUEST);
                        dialogIndexBook(v);
                    }
                }
            });
        } else {
            mOutlineButton.setVisibility(View.GONE);
        }

        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));

        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false)) {
            showButtons();
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false)) {
            searchModeOn();
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean("ReflowMode", false)) {
            reflowModeSet(true);
        }

        //TODO : Insert Pdf viewer with Ads
        LinearLayout mixlinear = setPdfView();

        // Stick the document view and the buttons overlay into a parent view
        final RelativeLayout layout = new RelativeLayout(this);
        layout.addView(mixlinear);
        layout.addView(mButtonsView);
        setContentView(layout);
        if (isProofing()) {
            //  go to the current page
            int currentPage = getIntent().getIntExtra("startingPage", 0);
            mDocView.setDisplayedViewIndex(currentPage);
        }
    }

    private List<EditorModel> loadEditor() {

        List<EditorModel> getlist = new ArrayList<>();

        int mockpage = 3;
        ArrayList<PointF> mockpoint = new ArrayList<>();
        mockpoint.add(new PointF(113.3858f, 902.19073f));
        mockpoint.add(new PointF(421.4338f, 902.19073f));
        mockpoint.add(new PointF(421.4338f, 874.0067f));
        mockpoint.add(new PointF(113.3858f, 874.0067f));
        Annotation mockanno = new Annotation(113.3858f, 874.0067f, 421.4338f, 902.19073f, 9);
        EditorModel model = new EditorModel(mockpage, mockpoint, mockanno);

        getlist.add(model);
        return getlist;
    }

    private void dialogIndexBook(View v) {
        try {
            final Dialog dialog = new Dialog(MuPDFActivity.this, R.style.FullHeightDialog);
            dialog.setContentView(R.layout.custom_dialog_fragment);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            //  dialog.setTitle(getString(R.string.Index_lang));
            //button
            okButtonDialog = (Button) dialog.findViewById(R.id.btn_ok_dialog);
            okButtonDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            //recyclerview
            indexRecyclerview = (RecyclerView) dialog.findViewById(R.id.recyclerView_Index);
            OutlineItem mItem[] = OutlineActivityData.get().items;
            indexBookAdapter = new IndexBookAdapter(v.getContext(), mItem, new IndexBookAdapter.OnItemClicked() {
                @Override
                public void onItemClick(View view, int position, int page) {
                    mDocView.setDisplayedViewIndex(page);
                    dialog.dismiss();
                }
            });
            //setlayout
            int heightland = (int) (getResources().getDisplayMetrics().heightPixels * 0.5);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.5);
            int checkscreen = (getResources().getDisplayMetrics().heightPixels);
            int sizeLayout = getScreensize();
            int orientationLayout = getOrientationLayout();

            RecyclerView.LayoutManager mLayoutmanager = new LinearLayoutManager(getApplicationContext());
            indexRecyclerview.setLayoutManager(mLayoutmanager);
            indexRecyclerview.setItemAnimator(new DefaultItemAnimator());
            indexRecyclerview.setAdapter(indexBookAdapter);

            if (sizeLayout == 1 && orientationLayout == 1) {
                //screen large & landscape
                if (indexBookAdapter.getItemCount() > 6)
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightland));

            } else if (sizeLayout == 1 && orientationLayout == 2) {
                //screen large & portait
                if (indexBookAdapter.getItemCount() > 10)
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

            } else if (sizeLayout == 2 && orientationLayout == 1) {
                //screen NORMAL & landscape
                if (indexBookAdapter.getItemCount() > 3)
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightland));
                if (checkscreen < 720 && indexBookAdapter.getItemCount() > 3) {
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (checkscreen * 0.35)));
                }

            } else if (sizeLayout == 2 && orientationLayout == 2) {
                //screen NORMAL & portait
                if (indexBookAdapter.getItemCount() > 5)
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

            } else if (sizeLayout == 3) {
                //screen XLARGE & landscape
                if (indexBookAdapter.getItemCount() > 8)
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

            } else if (sizeLayout == 3) {
                //screen XLARGE & portait
                if (indexBookAdapter.getItemCount() > 13)
                    indexRecyclerview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

            }

            dialog.show();

        } catch (Exception e) {
            Log.e("", e.toString());
        }
    }

    private int getOrientationLayout() {
        int orientation = getResources().getConfiguration().orientation;
        int orientationresult;
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return orientationresult = 1;

            case Configuration.ORIENTATION_PORTRAIT:
                return orientationresult = 2;

            default:
                return orientationresult = 2;
        }
    }

    private int getScreensize() {
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        int screenSizeResult;
        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                return screenSizeResult = 1;

            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return screenSizeResult = 2;

            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                return screenSizeResult = 3;
            default:
                return screenSizeResult = 1;
        }
    }

    private void updatePositionThumnailSeekbar(SeekBar seekBar, int progress) {

        int xPos = (((seekBar.getRight() - seekBar.getLeft()) * seekBar.getProgress()) /
                seekBar.getMax()) + seekBar.getLeft();
        int max = seekBar.getMax() + mPageSliderRes;
        thumnailview.setVisibility(View.VISIBLE);
        previewSeek.setVisibility(View.VISIBLE);

        if (progress >= 0 && progress <= max * 0.5f) {
            thumnailview.setX(xPos);
        }
        if (progress >= max * 0.5f && progress <= max * 0.1f) {
            thumnailview.setX(xPos - 7.5f);
        }
        if (progress >= max * 0.1f && progress <= max * 0.15f) {
            thumnailview.setX(xPos - 15);
        }
        if (progress >= max * 0.15f && progress <= max * 0.2f) {
            thumnailview.setX(xPos - 22.5f);
        }
        if (progress >= max * 0.2f && progress <= max * 0.25f) {
            thumnailview.setX(xPos - 30);
        }
        if (progress >= max * 0.25f && progress <= max * 0.3f) {
            thumnailview.setX(xPos - 37.5f);
        }
        if (progress >= max * 0.3f && progress <= max * 0.35f) {
            thumnailview.setX(xPos - 45);
        }
        if (progress >= max * 0.35f && progress <= max * 0.4f) {
            thumnailview.setX(xPos - 52.5f);
        }
        if (progress >= max * 0.4f && progress <= max * 0.45f) {
            thumnailview.setX(xPos - 60);
        }
        if (progress >= max * 0.45f && progress <= max * 0.5f) {
            thumnailview.setX(xPos - 67.5f);
        }
        if (progress >= max * 0.5f && progress <= max * 0.55f) {
            thumnailview.setX(xPos - 75);
        }
        if (progress >= max * 0.55f && progress <= max * 0.6f) {
            thumnailview.setX(xPos - 82.5f);
        }
        if (progress >= max * 0.6f && progress <= max * 0.65f) {
            thumnailview.setX(xPos - 90);
        }
        if (progress >= max * 0.65f && progress <= max * 0.7f) {
            thumnailview.setX(xPos - 97.5f);
        }
        if (progress >= max * 0.7f && progress <= max * 0.75f) {
            thumnailview.setX(xPos - 105);
        }
        if (progress >= max * 0.75f && progress <= max * 0.8f) {
            thumnailview.setX(xPos - 112.5f);
        }
        if (progress >= max * 0.8f && progress <= max * 0.85f) {
            thumnailview.setX(xPos - 120);
        }
        if (progress >= max * 0.85f && progress <= max * 0.9f) {
            thumnailview.setX(xPos - 127.5f);
        }
        if (progress >= max * 0.9f && progress <= max * 0.95f) {
            thumnailview.setX(xPos - 135);
        }
        if (progress >= max * 0.95f && progress <= max * 0.97f) {
            thumnailview.setX(xPos - 142.5f);
        }
        if (progress == max) {
            thumnailview.setX(xPos - 150f);
        }
        drawPreview(previewSeek, progress / 2);
    }

    @NonNull
    private LinearLayout setPdfView() {
        // insert view to activities

//        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                1.0f);

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        LinearLayout docView = new LinearLayout(this);
        docView.addView(mDocView);
        docView.setLayoutParams(param);
        docView.setOrientation(LinearLayout.VERTICAL);

        //TODO : Ads View
//        LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                6.0f);
//        LinearLayout AdsLinear;
//        AdsLinear = new LinearLayout(this);
//        AdsLinear.addView(AdsView);
//        AdsLinear.setLayoutParams(param2);
//        AdsLinear.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams param3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        LinearLayout mixlinear = new LinearLayout(this);
        mixlinear.addView(docView);
//        mixlinear.addView(AdsLinear);
        mixlinear.setLayoutParams(param3);
        mixlinear.setOrientation(LinearLayout.VERTICAL);
        return mixlinear;
    }

    private void saveBookMark() {
        SharedPreferences prefsBookmark = getBaseContext().getSharedPreferences(mBookId, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefsBookmark.edit();

        SharedPreferences prefsBookmark_boolean = getBaseContext().getSharedPreferences(mBookId + "_boolean", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit_boolean = prefsBookmark_boolean.edit();

        boolean isbookMark = prefsBookmark_boolean.getBoolean(String.format(
                String.valueOf(mDocView.getDisplayedViewIndex() + 1)) + "_boolean", false);
        Log.d("IS BOOKMARK >> ,", isbookMark + "");

        if (isbookMark) {

            edit_boolean.putBoolean(String.format(
                    String.valueOf(mDocView.getDisplayedViewIndex() + 1) + "_boolean"), false);
            edit_boolean.remove(String.valueOf(mDocView.getDisplayedViewIndex() + 1) + "_boolean");
            edit.remove(String.valueOf(mDocView.getDisplayedViewIndex() + 1));

            edit.commit();
            edit_boolean.commit();

            Toast.makeText(getApplicationContext(), R.string.removebookmark, Toast.LENGTH_SHORT).show();

        } else {
            dialogAddBookMark();
        }

        displayBookmarkButton();
    }

    private void dialogAddBookMark() {

        final SharedPreferences prefsBookmark = getBaseContext().getSharedPreferences(mBookId, Context.MODE_PRIVATE);
        final SharedPreferences.Editor edit = prefsBookmark.edit();

        final SharedPreferences prefsBookmark_boolean = getBaseContext().getSharedPreferences(mBookId + "_boolean", Context.MODE_PRIVATE);
        final SharedPreferences.Editor edit_boolean = prefsBookmark_boolean.edit();

        final Dialog dialog = new Dialog(this, R.style.FullHeightDialog);
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
                if (null != pageDescription.getLayout() && pageDescription.getLayout().getLineCount() > 3) {
                    pageDescription.getText().delete(pageDescription.getText().length() - 1, pageDescription.getText().length());
                }
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.button_cancel_dialog);
        Button saveButton = (Button) dialog.findViewById(R.id.button_save_dialog);


        pageNumber.setText(String.format(
                String.valueOf(mDocView.getDisplayedViewIndex() + 1)));

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                edit_boolean.putBoolean(String.format(
                        String.valueOf(mDocView.getDisplayedViewIndex() + 1) + "_boolean"), true);
                edit.putString(String.valueOf(mDocView.getDisplayedViewIndex() + 1), pageDescription.getText().toString());

                edit.commit();
                edit_boolean.commit();
                Toast.makeText(getApplicationContext(), R.string.savecomplete, Toast.LENGTH_SHORT).show();
                displayBookmarkButton();
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void displayBookmarkButton() {

        SharedPreferences prefsBookmark_boolean = getBaseContext().getSharedPreferences(mBookId + "_boolean", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit_boolean = prefsBookmark_boolean.edit();

        boolean isbookMark = prefsBookmark_boolean.getBoolean(String.format(
                String.valueOf(mDocView.getDisplayedViewIndex() + 1)) + "_boolean", false);
        if (isbookMark) {
            mBookmarkButton.setImageResource(R.drawable.ic_bookmarked);

        } else {
            mBookmarkButton.setImageResource(R.drawable.ic_bookmark);
        }
    }

    private void DetailBookmark() {

        Intent intent = new Intent(getBaseContext(), Bookmark.class);
        intent.putExtra("URI_PATH", mPathFile.toString());
        intent.putExtra("mFileName", mFileName);
        intent.putExtra("BOOK_ID", mBookId);
        intent.putExtra("BOOK_PASSWORD", mSpritPassword);
//        int pagePosition = data.getIntExtra("PAGE_RESULT", mDocView.getDisplayedViewIndex());
        intent.putExtra("NUMBER_PAGE", String.format(String.valueOf(mDocView.getDisplayedViewIndex() + 1)));
        startActivityForResult(intent, BOOKMARK_RESULT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= 0) mDocView.setDisplayedViewIndex(resultCode);
                break;
            case PRINT_REQUEST:
                if (resultCode == RESULT_CANCELED) showInfo(getString(R.string.print_failed));
                break;
            case FILEPICK_REQUEST:
                if (mFilePicker != null && resultCode == RESULT_OK)
                    mFilePicker.onPick(data.getData());
            case BOOKMARK_RESULT:
                if (resultCode == RESULT_OK) {
                    int pagePosition = data.getIntExtra("PAGE_RESULT", mDocView.getDisplayedViewIndex());
//                    Log.d("pagePosition : ", pagePosition + "");
                    mDocView.setDisplayedViewIndex(pagePosition);
                }
                break;
            case PROOF_REQUEST:
                //  we're returning from a proofing activity

                if (mProofFile != null) {
                    core.endProof(mProofFile);
                    mProofFile = null;
                }

                //  return the top bar to default
                mTopBarMode = TopBarMode.Main;
                mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public Object onRetainNonConfigurationInstance() {
        MuPDFCore mycore = core;
        core = null;
        return mycore;
    }

    private void reflowModeSet(boolean reflow) {
        mReflow = reflow;
        mDocView.setAdapter(
                mReflow ? new MuPDFReflowAdapter(this, core) : new MuPDFPageAdapter(this, this, core, loadEditor()));
        mReflowButton.setColorFilter(
                mReflow ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
        setButtonEnabled(mAnnotButton, !reflow);
        setButtonEnabled(mSearchButton, !reflow);
        if (reflow) setLinkHighlight(false);
        setButtonEnabled(mLinkButton, !reflow);
        setButtonEnabled(mMoreButton, !reflow);
        mDocView.refresh(mReflow);
    }

    private void toggleReflow() {
        reflowModeSet(!mReflow);
        showInfo(mReflow ? getString(R.string.entering_reflow_mode)
                : getString(R.string.leaving_reflow_mode));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mBookId != null && mDocView != null) {
            outState.putString("FileName", mFileName);
            outState.putString("BOOK_ID", mBookId);
            outState.putString("BOOK_NAME", mBookName);
            outState.putString("BOOK_PASSWORD", mSpritPassword);
            outState.putString("Pathfile", mPathFile.toString());
            // Store current page in the prefs ag   ainst the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }

        if (!mButtonsVisible) outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == TopBarMode.Search) outState.putBoolean("SearchMode", true);

        if (mReflow) outState.putBoolean("ReflowMode", true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchTask != null) mSearchTask.stop();

        if (mFileName != null && mDocView != null) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }
    }

    public void onDestroy() {
        if (mDocView != null) {
            mDocView.applyToChildren(new ReaderView.ViewMapper() {
                void applyToView(View view) {
                    ((MuPDFView) view).releaseBitmaps();
                }
            });
        }
        if (core != null) core.onDestroy();
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        core = null;
        thumnailview.setVisibility(View.GONE);
        previewSeek.setVisibility(View.GONE);
        super.onDestroy();
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(
                enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
    }

    private void setLinkHighlight(boolean highlight) {
        mLinkHighlight = highlight;
        // LINK_COLOR tint
        mLinkButton.setColorFilter(
                highlight ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
        // Inform pages of the change.
        mDocView.setLinksEnabled(highlight);
    }

    private void showButtons() {
        if (core == null) return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);
            if (mTopBarMode == TopBarMode.Search) {
                mSearchText.requestFocus();
                showKeyboard();
            }

            Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageSlider.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageNumberView.setVisibility(View.VISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();

            Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.INVISIBLE);
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageNumberView.setVisibility(View.INVISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageSlider.setVisibility(View.INVISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            //Focus on EditTextWidget
            mSearchText.requestFocus();
            showKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }
    }

    private void searchModeOff() {
        if (mTopBarMode == TopBarMode.Search) {
            mTopBarMode = TopBarMode.Main;
            hideKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            SearchTaskResult.set(null);
            // Make the ReaderView act on the change to mSearchTaskResult
            // via overridden onChildSetup method.
            mDocView.resetupChildren();
        }
    }

    private void updatePageNumView(int index) {
        if (core == null) return;
        mPageNumberView.setText(String.format("%d / %d", index + 1, core.countPages()));
    }

    private void printDoc() {
        if (!core.fileFormat().startsWith("PDF")) {
            showInfo(getString(R.string.format_currently_not_supported));
            return;
        }

        Intent myIntent = getIntent();
        Uri docUri = myIntent != null ? myIntent.getData() : null;

        if (docUri == null) {
            showInfo(getString(R.string.print_failed));
        }

        if (docUri.getScheme() == null) docUri = Uri.parse("file://" + docUri.toString());

        Intent printIntent = new Intent(this, PrintDialogActivity.class);
        printIntent.setDataAndType(docUri, "aplication/pdf");
        printIntent.putExtra("title", mFileName);
        startActivityForResult(printIntent, PRINT_REQUEST);
    }

    private void showInfo(String message) {
        mInfoView.setText(message);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            SafeAnimatorInflater safe =
                    new SafeAnimatorInflater((Activity) this, R.animator.info, (View) mInfoView);
        } else {
            mInfoView.setVisibility(View.VISIBLE);
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    mInfoView.setVisibility(View.INVISIBLE);
                }
            }, 500);
        }
    }

    private void makeButtonsView() {
        mButtonsView = getLayoutInflater().inflate(R.layout.buttons, null);
        mFilenameView = (TextView) mButtonsView.findViewById(R.id.docNameText);
        mPageSlider = (SeekBar) mButtonsView.findViewById(R.id.pageSlider);
        mPageNumberView = (TextView) mButtonsView.findViewById(R.id.pageNumber);
        mInfoView = (TextView) mButtonsView.findViewById(R.id.info);
        mSearchButton = (ImageButton) mButtonsView.findViewById(R.id.searchButton);
        mReflowButton = (ImageButton) mButtonsView.findViewById(R.id.reflowButton);
        mOutlineButton = (ImageButton) mButtonsView.findViewById(R.id.outlineButton);
        mAnnotButton = (ImageButton) mButtonsView.findViewById(R.id.editAnnotButton);
        mAnnotTypeText = (TextView) mButtonsView.findViewById(R.id.annotType);
        mTopBarSwitcher = (ViewAnimator) mButtonsView.findViewById(R.id.switcher);
        mSearchBack = (ImageButton) mButtonsView.findViewById(R.id.searchBack);
        mSearchFwd = (ImageButton) mButtonsView.findViewById(R.id.searchForward);
        mSearchText = (EditText) mButtonsView.findViewById(R.id.searchText);
        mLinkButton = (ImageButton) mButtonsView.findViewById(R.id.linkButton);
        mMoreButton = (ImageButton) mButtonsView.findViewById(R.id.moreButton);
        mProofButton = (ImageButton) mButtonsView.findViewById(R.id.proofButton);
        mSepsButton = (ImageButton) mButtonsView.findViewById(R.id.sepsButton);
        mTopBarSwitcher.setVisibility(View.INVISIBLE);
        mPageNumberView.setVisibility(View.INVISIBLE);
        mInfoView.setVisibility(View.INVISIBLE);

        mPageSlider.setVisibility(View.INVISIBLE);
        if (!core.gprfSupported()) {
            mProofButton.setVisibility(View.GONE);
        }
        mPageSlider.setPadding(40, 12, 40, 8);
        mSepsButton.setVisibility(View.INVISIBLE);


        //Ads View
        AdsView = getLayoutInflater().inflate(R.layout.adview, null);
        viewPagerAds = (ViewPager) AdsView.findViewById(R.id.viewpager_ads);

        //get url for ads
        String url1 = "https://i.imgur.com/mPm2Ovn.png";
        String url2 = "https://i.imgur.com/hzSwtc3.jpg";
        String url3 = "http://static.adweek.com/adweek.com-prod/wp-content/uploads/files/digital-advertising-ep-2016.png";

        viewPagerAds.setAdapter(new CustomPagerAdapter(this, url1, url2, url3));
        pageIndicatorView = (PageIndicatorView) AdsView.findViewById(R.id.IndicatorView);
        pageIndicatorView.setViewPager(viewPagerAds);

        //TODO : Detail bookmark Button
        mBookmarkDeTailButton = (ImageButton) mButtonsView.findViewById(R.id.bookmark_DetailButton);
        mBookmarkButton = (ImageButton) mButtonsView.findViewById(R.id.bookmarkButton);
        //TODO: thumnail seekbar
        thumnailview = (RelativeLayout) mButtonsView.findViewById(R.id.seek_Thumbnail);
        previewSeek = (ImageView) mButtonsView.findViewById(R.id.image_Thumnail);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    public void OnMoreButtonClick(View v) {
        mTopBarMode = TopBarMode.More;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelMoreButtonClick(View v) {
        mTopBarMode = TopBarMode.Main;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnPrintButtonClick(View v) {
        printDoc();
    }

    //  start a proof activity with the given resolution.
    public void proofWithResolution(int resolution) {
        mProofFile = core.startProof(resolution);
        Uri uri = Uri.parse("file://" + mProofFile);
        Intent intent = new Intent(this, MuPDFActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        // add the current page so it can be found when the activity is running
        intent.putExtra("startingPage", mDocView.getDisplayedViewIndex());
        startActivityForResult(intent, PROOF_REQUEST);
    }

    public void OnProofButtonClick(final View v) {
        //  set up the menu or resolutions.
        final PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Select a resolution:");
        popup.getMenu().add(0, 72, 0, "72");
        popup.getMenu().add(0, 96, 0, "96");
        popup.getMenu().add(0, 150, 0, "150");
        popup.getMenu().add(0, 300, 0, "300");
        popup.getMenu().add(0, 600, 0, "600");
        popup.getMenu().add(0, 1200, 0, "1200");
        popup.getMenu().add(0, 2400, 0, "2400");

        //  prevent the first item from being dismissed.
        //  is there not a better way to do this?  It requires minimum API 14
        MenuItem item = popup.getMenu().getItem(0);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(v.getContext()));
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return false;
            }
        });

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id != 1) {
                    //  it's a resolution.  The id is also the resolution value
                    proofWithResolution(id);
                    return true;
                }
                return false;
            }
        });

        popup.show();
    }

    public void OnSepsButtonClick(final View v) {
        if (isProofing()) {

            //  get the current page
            final int currentPage = mDocView.getDisplayedViewIndex();

            //  buid a popup menu based on the given separations
            final PopupMenu menu = new PopupMenu(this, v);

            //  This makes the popup menu display icons, which by default it does not do.
            //  I worry that this relies on the internals of PopupMenu, which could change.
            try {
                Field[] fields = menu.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(menu);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //  get the maximum number of seps on any page.
            //  We use this to dimension an array further down
            int maxSeps = 0;
            int numPages = core.countPages();
            for (int page = 0; page < numPages; page++) {
                int numSeps = core.getNumSepsOnPage(page);
                if (numSeps > maxSeps) maxSeps = numSeps;
            }

            //  if this is the first time, create the "enabled" array
            if (mSepEnabled == null) {
                mSepEnabled = new boolean[numPages][maxSeps];
                for (int page = 0; page < numPages; page++) {
                    for (int i = 0; i < maxSeps; i++)
                        mSepEnabled[page][i] = true;
                }
            }

            //  count the seps on this page
            int numSeps = core.getNumSepsOnPage(currentPage);

            //  for each sep,
            for (int i = 0; i < numSeps; i++) {

                //				//  Robin use this to skip separations
                //				if (i==12)
                //					break;

                //  get the name
                Separation sep = core.getSep(currentPage, i);
                String name = sep.name;

                //  make a checkable menu item with that name
                //  and the separation index as the id
                MenuItem item = menu.getMenu().add(0, i, 0, name + "    ");
                item.setCheckable(true);

                //  set an icon that's the right color
                int iconSize = 48;
                int alpha = (sep.rgba >> 24) & 0xFF;
                int red = (sep.rgba >> 16) & 0xFF;
                int green = (sep.rgba >> 8) & 0xFF;
                int blue = (sep.rgba >> 0) & 0xFF;
                int color = (alpha << 24) | (red << 16) | (green << 8) | (blue << 0);

                ShapeDrawable swatch = new ShapeDrawable(new RectShape());
                swatch.setIntrinsicHeight(iconSize);
                swatch.setIntrinsicWidth(iconSize);
                swatch.setBounds(new Rect(0, 0, iconSize, iconSize));
                swatch.getPaint().setColor(color);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                item.setIcon(swatch);

                //  check it (or not)
                item.setChecked(mSepEnabled[currentPage][i]);

                //  establishing a menu item listener
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        //  someone tapped a menu item.  get the ID
                        int sep = item.getItemId();

                        //  toggle the sep
                        mSepEnabled[currentPage][sep] = !mSepEnabled[currentPage][sep];
                        item.setChecked(mSepEnabled[currentPage][sep]);
                        core.controlSepOnPage(currentPage, sep, !mSepEnabled[currentPage][sep]);

                        //  prevent the menu from being dismissed by these items
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                        item.setActionView(new View(v.getContext()));
                        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                            @Override
                            public boolean onMenuItemActionExpand(MenuItem item) {
                                return false;
                            }

                            @Override
                            public boolean onMenuItemActionCollapse(MenuItem item) {
                                return false;
                            }
                        });
                        return false;
                    }
                });

                //  tell core to enable or disable each sep as appropriate
                //  but don't refresh the page yet.
                core.controlSepOnPage(currentPage, i, !mSepEnabled[currentPage][i]);
            }

            //  add one for done
            MenuItem itemDone = menu.getMenu().add(0, 0, 0, "Done");
            itemDone.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    //  refresh the view
                    mDocView.refresh(false);
                    return true;
                }
            });

            //  show the menu
            menu.show();
        }
    }

    public void OnCopyTextButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.CopyText;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(getString(R.string.copy_text));
        showInfo(getString(R.string.select_text));
    }

    public void OnEditAnnotButtonClick(View v) {
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelAnnotButtonClick(View v) {
        mTopBarMode = TopBarMode.More;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnHighlightButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Highlight;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.highlight);
        showInfo(getString(R.string.select_text));
    }

    public void OnUnderlineButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Underline;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.underline);
        showInfo(getString(R.string.select_text));
    }

    public void OnStrikeOutButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.StrikeOut;
        mDocView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.strike_out);
        showInfo(getString(R.string.select_text));
    }

    public void OnInkButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Ink;
        mDocView.setMode(MuPDFReaderView.Mode.Drawing);
        mAnnotTypeText.setText(R.string.ink);
        showInfo(getString(R.string.draw_annotation));
    }

    public void OnCancelAcceptButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null) {
            pageView.deselectText();
            pageView.cancelDraw();
        }
        mDocView.setMode(MuPDFReaderView.Mode.Viewing);
        switch (mAcceptMode) {
            case CopyText:
                mTopBarMode = TopBarMode.More;
                break;
            default:
                mTopBarMode = TopBarMode.Annot;
                break;
        }
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnAcceptButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        boolean success = false;
        switch (mAcceptMode) {
            case CopyText:
                if (pageView != null) success = pageView.copySelection();
                mTopBarMode = TopBarMode.More;
                showInfo(success ? getString(R.string.copied_to_clipboard)
                        : getString(R.string.no_text_selected));
                break;

            case Highlight:
                Log.d("FLUKE", "success : " + pageView);
                if (pageView != null) success = pageView.markupSelection(Annotation.Type.HIGHLIGHT);
                mTopBarMode = TopBarMode.Annot;
                if (!success) showInfo(getString(R.string.no_text_selected));
                break;

            case Underline:
                if (pageView != null) success = pageView.markupSelection(Annotation.Type.UNDERLINE);
                mTopBarMode = TopBarMode.Annot;
                if (!success) showInfo(getString(R.string.no_text_selected));
                break;

            case StrikeOut:
                if (pageView != null) success = pageView.markupSelection(Annotation.Type.STRIKEOUT);
                mTopBarMode = TopBarMode.Annot;
                if (!success) showInfo(getString(R.string.no_text_selected));
                break;

            case Ink:
                if (pageView != null) success = pageView.saveDraw();
                mTopBarMode = TopBarMode.Annot;
                if (!success) showInfo(getString(R.string.nothing_to_save));
                break;
        }
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mDocView.setMode(MuPDFReaderView.Mode.Viewing);
    }

    public void OnCancelSearchButtonClick(View v) {
        searchModeOff();
    }

    public void OnDeleteButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null) pageView.deleteSelectedAnnotation();
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelDeleteButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
        if (pageView != null) pageView.deselectAnnotation();
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(mSearchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private void search(int direction) {
        hideKeyboard();
        int displayPage = mDocView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
    }

    @Override
    public boolean onSearchRequested() {
        if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOn();
        }
        return super.onSearchRequested();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOff();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        if (core != null) {
            core.startAlerts();
            createAlertWaiter();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (core != null) {
            destroyAlertWaiter();
            core.stopAlerts();
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (core != null && core.hasChanges()) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == AlertDialog.BUTTON_POSITIVE) {
//                        core.save();
                        MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
                        if (pageView != null) {
                            try {
                                Log.d("has change","model"+new Gson().toJson( pageView.getEditorScriptToSent()));
                            } catch (Exception e) {
                                Log.d("has change","error "+e.toString());
                            }
                        }
                    }

                    finish();
                }
            };
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle("MuPDF");
            alert.setMessage(getString(R.string.document_has_changes_save_them_));
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
            alert.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void performPickFor(FilePicker picker) {
        mFilePicker = picker;
        Intent intent = new Intent(this, ChoosePDFActivity.class);
        intent.setAction(ChoosePDFActivity.PICK_KEY_FILE);
        startActivityForResult(intent, FILEPICK_REQUEST);
    }

    private File cacheDir;

    public void drawPreview(ImageView imageView, int position) {
        if (cancelPotentialWork(imageView, position)) {
            Bitmap mLoadingBitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.bookloading);
            final BitmapTask task = new BitmapTask(position, imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(
                    getResources(), mLoadingBitmap, task);
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

    public class BitmapTask extends AsyncTask<Void, Void, Bitmap> {
        private int position;
        ImageView preView;

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


    }

    private Bitmap getBitmapCached(final int position) {

        String cacheName = mBookId + "_" + position + ".jpg";
        cacheDir = new CacheDirectoryHelper(getBaseContext()).getCacheDir(mBookId);

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
