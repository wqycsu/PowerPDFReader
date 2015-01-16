package com.csu.powerpdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.csu.utils.FileUtils;
import com.csu.utils.InsertWatermarkDialog;
import com.csu.utils.MediaPlayerDialog;

class ThreadPerTaskExecutor implements Executor {
	public void execute(Runnable r) {
		new Thread(r).start();
	}
}

public class MuPDFActivity extends Activity
{
	/* The core rendering instance */
	enum TopBarMode {Main, Search, Annot, Delete, More, Accept};
	enum AcceptMode {Highlight, Underline, StrikeOut, Ink, CopyText};
	
	private final int    OUTLINE_REQUEST=0;
	private final int    PRINT_REQUEST=1;
	private MuPDFCore    core;
	private String       mFileName;
	private MuPDFReaderView mDocView;
	private View         mButtonsView;
	private boolean      mButtonsVisible;
	private EditText     mPasswordView;
	private TextView     mFilenameView;
	private SeekBar      mPageSlider;
	private int          mPageSliderRes;
	private TextView     mPageNumberView;
	private TextView     mInfoView;
	private ImageButton  mSearchButton;
	private ImageButton  mCopyTextButton;
	private ImageButton  mOutlineButton;
	private ImageButton	mMoreButton;
	private TextView     mAnnotTypeText;
	private ImageButton mAnnotButton;
	private ViewAnimator mTopBarSwitcher;
	private ImageButton  mLinkButton;
	private TopBarMode   mTopBarMode = TopBarMode.Main;
	private AcceptMode   mAcceptMode;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private EditText     mSearchText;
	private SearchTask   mSearchTask;
	private AlertDialog.Builder mAlertBuilder;
	private boolean    mLinkHighlight = false;
	private final Handler mHandler = new Handler();
	private boolean mAlertsActive= false;
	private boolean mReflow = false;
	private AsyncTask<Void,Void,MuPDFAlert> mAlertTask;
	private AlertDialog mAlertDialog;
	
	Display display = null;
	MediaPlayerDialog audioPlayDialog = null;
	MediaPlayer player = null;
	ImageView play_pause;
	SeekBar seekBar;
	Handler handler;
	Runnable runnable;
	//watermark
	InsertWatermarkDialog dialog;
	private final static String TAG = "weiquanyun";
	private static final int PHOTO_PICKED_DATA = 3021;
	//audio
	private final static int PICK_AUDIO = 1111;
	private final static int RECORD_AUDIO = 2222;
	String local_audio_path;
	String record_audio_path;
	String strRecorderPath;
	String sound_icon_path;
	//audio insert dialog
	Dialog insertAudioDialog = null;
	int audioX = 0;
	int audioY = 0;
	private String watermarkPath;
	private Bitmap watermarkBitmap;
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
		mAlertTask = new AsyncTask<Void,Void,MuPDFAlert>() {

			@Override
			protected MuPDFAlert doInBackground(Void... arg0) {
				if (!mAlertsActive)
					return null;

				return core.waitForAlert();
			}

			@Override
			protected void onPostExecute(final MuPDFAlert result) {
				// core.waitForAlert may return null when shutting down
				if (result == null)
					return;
				final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
				for(int i = 0; i < 3; i++)
					pressed[i] = MuPDFAlert.ButtonPressed.None;
				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mAlertDialog = null;
						if (mAlertsActive) {
							int index = 0;
							switch (which) {
							case AlertDialog.BUTTON_POSITIVE: index=0; break;
							case AlertDialog.BUTTON_NEGATIVE: index=1; break;
							case AlertDialog.BUTTON_NEUTRAL: index=2; break;
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
				switch (result.iconType)
				{
				case Error:
					break;
				case Warning:
					break;
				case Question:
					break;
				case Status:
					break;
				}
				switch (result.buttonGroupType)
				{
				case OkCancel:
					mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), listener);
					pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
				case Ok:
					mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay), listener);
					pressed[0] = MuPDFAlert.ButtonPressed.Ok;
					break;
				case YesNoCancel:
					mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.cancel), listener);
					pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
				case YesNo:
					mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
					pressed[0] = MuPDFAlert.ButtonPressed.Yes;
					mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
					pressed[1] = MuPDFAlert.ButtonPressed.No;
					break;
				}
				mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
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

	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = new String(lastSlashPos == -1
					? path
					: path.substring(lastSlashPos+1));
		System.out.println("Trying to open "+path);
		try
		{
			core = new MuPDFCore(this, path);
			// New file: drop the old outline data
			OutlineActivityData.set(null);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}

	private MuPDFCore openBuffer(byte buffer[])
	{
		System.out.println("Trying to open byte buffer");
		try
		{
			core = new MuPDFCore(this, buffer);
			// New file: drop the old outline data
			OutlineActivityData.set(null);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mAlertBuilder = new AlertDialog.Builder(this);
		display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		
		if (core == null) {
			core = (MuPDFCore)getLastNonConfigurationInstance();

			if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
				mFileName = savedInstanceState.getString("FileName");
			}
		}
		if (core == null) {
			Intent intent = getIntent();
			byte buffer[] = null;
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				if (uri.toString().startsWith("content://")) {
					// Handle view requests from the Transformer Prime's file manager
					// Hopefully other file managers will use this same scheme, if not
					// using explicit paths.
					Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
					if (cursor.moveToFirst()) {
						String str = cursor.getString(0);
						String reason = null;
						if (str == null) {
							try {
								InputStream is = getContentResolver().openInputStream(uri);
								int len = is.available();
								buffer = new byte[len];
								is.read(buffer, 0, len);
								is.close();
							}
							catch (java.lang.OutOfMemoryError e)
							{
								System.out.println("Out of memory during buffer reading");
								reason = e.toString();
							}
							catch (Exception e) {
								reason = e.toString();
							}
							if (reason != null)
							{
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
						} else {
							uri = Uri.parse(str);
						}
					}
				}
				if (buffer != null) {
					core = openBuffer(buffer);
				} else {
					core = openFile(Uri.decode(uri.getEncodedPath()));
				}
				SearchTaskResult.set(null);
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return;
			}
			if (core != null && core.countPages() == 0)
			{
				core = null;
			}
		}
		if (core == null)
		{
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle(R.string.cannot_open_document);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.show();
			return;
		}

		createUI(savedInstanceState);
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
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view
		mDocView = new MuPDFReaderView(this) {
			@Override
			protected void onMoveToChild(int i) {
				if (core == null)
					return;
				mPageNumberView.setText(String.format("%d / %d", i + 1,
						core.countPages()));
				mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
				mPageSlider.setProgress(i * mPageSliderRes);
				super.onMoveToChild(i);
			}

			@Override
			protected void onTapMainDocArea() {
				if (!mButtonsVisible) {
					showButtons();
				} else {
					if (mTopBarMode == TopBarMode.Main)
						hideButtons();
				}
			}

			@Override
			protected void onDocMotion() {
				hideButtons();
			}

			@Override
			protected void onHit(Hit item) {
				Log.d("weiquanyun","onHit-->"+item.name()+" mTopBarMode-->"+mTopBarMode);
				switch (mTopBarMode) {
				case Main:
					final MuPDFPageView pageview = (MuPDFPageView)mDocView.getDisplayedView();
					Annotation annot = pageview.getSelectedAnnotation();
					if(annot!=null&&annot.type==Annotation.Type.SCREEN)
					{
						Log.d("weiquanyun","annot-->"+annot.type);
						Point point = pageview.getPoint();
//						float scale = ((ReaderView)mDocView).getScale();
//						int w = mDocView.getDisplayedView().getMeasuredWidth();
//						int h = mDocView.getDisplayedView().getMeasuredHeight();
						//compute the original width and height
//						o_pageWidth = (int)(w/scale);
//						o_pageHeight = (int)(h/scale);
//						Log.d("weiquanyun","onHit.scale-->"+scale);
//						Log.d("weiquanyun","annotation.point-->"+(int)(annot.getLeft())+"  "+(int)(annot.getTop()));
						byte[] bytes = core.playAudio(pageview.mPageNumber, (int)(annot.getLeft()), (int)(annot.getTop()));
						if(bytes==null||bytes.length==0){
							Toast.makeText(getApplicationContext(), "音频加载失败", Toast.LENGTH_LONG).show();
							return;
						}
//						Log.d("weiquanyun","bytes-->"+bytes.length/8);
						String fileName = mFileName.substring(0,mFileName.lastIndexOf('.')-1);
						File file = new File(FileUtils.APP_PATH+"/"+fileName+pageview.mPageNumber+""+point.x+""+""+point.y+".mp3");
						if(!file.exists()){
							FileOutputStream output = null;
							try {
								output = new FileOutputStream(file);
								output.write(bytes);
								output.close();
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								Log.d("weiquanyun","1-"+e.toString());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								Log.d("weiquanyun","2-"+e.toString());
							}
						}
						if(file.length()==0)
						{
							Toast.makeText(getApplicationContext(), "音频加载失败", Toast.LENGTH_LONG).show();
							return;
						}
						int dialogWidth = (int)(display.getWidth()*0.8);
						int dialogHeight = (int)(display.getHeight()*0.2);
						Log.d("weiquanyun","dialogWidth = "+dialogWidth);
						Log.d("weiquanyun","dialogHeight = "+dialogHeight);
						player = new MediaPlayer();
						audioPlayDialog = new MediaPlayerDialog(MuPDFActivity.this, dialogWidth, dialogHeight);
						audioPlayDialog.show();
						audioPlayDialog.setOnDismissListener(new OnDismissListener(){

							@Override
							public void onDismiss(DialogInterface dialog) {
								// TODO Auto-generated method stub
								handler.removeCallbacks(runnable);
								player.stop();
								player.release();
								pageview.setSelectedAnnotation(null);
								onHit(Hit.Nothing);
							}
							
						});
						play_pause = audioPlayDialog.getPlayPause();
						seekBar = audioPlayDialog.getSeekBar();
						try {
							player.setDataSource(file.getAbsolutePath());
							player.prepare();
							seekBar.setMax(player.getDuration());
							play_pause.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View v) {
									// TODO Auto-generated method stub
									Log.d("weiquanyun","click-->"+player.isPlaying());
									if(player.isPlaying())
									{
										play_pause.setImageResource(R.drawable.edit_audio_play_button);
										player.pause();
									}
									else
									{
										player.start();
										handler.post(runnable);
										play_pause.setImageResource(R.drawable.edit_audio_pause_button);
									}
								}
								
							});
							handler = new Handler(){
								@Override
								public void handleMessage(Message msg){
									
								}
							};
							runnable = new Runnable(){

								@Override
								public void run() {
									// TODO Auto-generated method stub
									seekBar.setProgress(player.getCurrentPosition());
									/**
									 * Causes the Runnable r to be added to the message queue, 
									 * to be run after the specified amount of time elapses. 
									 * The runnable will be run on the thread to which this 
									 * handler is attached.
									 */
									handler.postDelayed(runnable, 100);
								}
								
							};
							seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

								@Override
								public void onProgressChanged(SeekBar seekBar,
										int progress, boolean fromUser) {
									// TODO Auto-generated method stub
									if(fromUser)
									{
										seekBar.setProgress(progress);
										player.seekTo(progress);
									}
									
								}

								@Override
								public void onStartTrackingTouch(SeekBar seekBar) {
									// TODO Auto-generated method stub
									
								}

								@Override
								public void onStopTrackingTouch(SeekBar seekBar) {
									// TODO Auto-generated method stub
									if(seekBar.getProgress()==player.getDuration())
									{
										player.stop();
										player.release();
									}
								}
								
							});
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							Log.d("weiquanyun","3-"+e.toString());
						} catch (SecurityException e) {
							// TODO Auto-generated catch block
							Log.d("weiquanyun","4-"+e.toString());
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							Log.d("weiquanyun","5-"+e.toString());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Log.d("weiquanyun","6-"+e.toString());
						}
						
					}
					break;
				case Annot:
					Log.d("weiquanyun","Hit.Annotation-->--->-->"+Hit.Annotation);
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
					if (pageView != null)
						pageView.deselectAnnotation();
//					if (!mButtonsVisible) {
//						showButtons();
//					}else{
//						if (mTopBarMode == TopBarMode.Main)
//							hideButtons();
//					}
					break;
				}
			}
		};
		mDocView.setAdapter(new MuPDFPageAdapter(this, core));

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
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;

		// Set the file-name text
		mFilenameView.setText(mFileName);

		// Activate the seekbar
		mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
			}
		});

		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
			}
		});
		
		//copy text
		mCopyTextButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				OnCopyTextButtonClick(v);
			}
			
		});
		
		// Activate the reflow button
//		mReflowButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
////				toggleReflow();
////		
//
//			}
//		});

		if (core.fileFormat().startsWith("PDF"))
		{
			mAnnotButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mTopBarMode = TopBarMode.Annot;
					mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
				}
			});
		}
		else
		{
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
				if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
					SearchTaskResult.set(null);
					mDocView.resetupChildren();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
		});

		//React to Done button on keyboard
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search(1);
				return false;
			}
		});

		mSearchText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
					search(1);
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

		if (core.hasOutline()) {
			mOutlineButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					OutlineItem outline[] = core.getOutline();
					if (outline != null) {
						OutlineActivityData.get().items = outline;
						Intent intent = new Intent(MuPDFActivity.this, OutlineActivity.class);
						startActivityForResult(intent, OUTLINE_REQUEST);
					}
				}
			});
		} else {
			mOutlineButton.setVisibility(View.GONE);
		}

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		mDocView.setDisplayedViewIndex(prefs.getInt("page"+mFileName, 0));

		if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
			showButtons();

		if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
			searchModeOn();

		if(savedInstanceState != null && savedInstanceState.getBoolean("ReflowMode", false))
			reflowModeSet(true);

		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.addView(mDocView);
		layout.addView(mButtonsView);
		layout.setBackgroundColor(Color.parseColor("#FF808080"));
		setContentView(layout);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case OUTLINE_REQUEST:
			if (resultCode >= 0)
				mDocView.setDisplayedViewIndex(resultCode);
			break;
		case PRINT_REQUEST:
			if (resultCode == RESULT_CANCELED)
				showInfo(getString(R.string.print_failed));
			break;
		case PHOTO_PICKED_DATA:
			if (resultCode != 0) {
				int degree = 0;
				watermarkPath = null;
				Uri uri = null;
				if (requestCode == PHOTO_PICKED_DATA) {
					// 获取图片的路径，及图片的旋转角度
					uri = data.getData();
					if (uri != null) {
						String[] proj = { MediaStore.Images.Media.DATA };
						Cursor cursor = null;
						try {
							cursor = managedQuery(uri, proj, null, null,null);
							int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
							cursor.moveToFirst();
							watermarkPath = cursor.getString(column_index);
						} catch (Exception e) {

						} finally {
							if (cursor != null && Integer.parseInt(Build.VERSION.SDK) < 14) {
								cursor.close();
							}
						}
					} 
				}
				ExifInterface exifInterface = null;
				if(watermarkPath !=null){
					try {
						exifInterface = new ExifInterface(watermarkPath);
						int tag = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
		
						if (tag == ExifInterface.ORIENTATION_ROTATE_90) {
							degree = 90;
						} else if (tag == ExifInterface.ORIENTATION_ROTATE_180) {
							degree = 180;
						} else if (tag == ExifInterface.ORIENTATION_ROTATE_270) {
							degree = 270;
						}
					} catch (IOException e) {
		
					} catch (Exception e){
						
					}
				}
				watermarkBitmap = null;
				if (requestCode == PHOTO_PICKED_DATA) {
					ContentResolver cr = getContentResolver();
					try {
						BitmapFactory.Options opts = new BitmapFactory.Options();
						opts.inJustDecodeBounds = true;
						BitmapFactory.decodeStream(cr.openInputStream(uri), null, opts);
						// opts.inSampleSize = 10;
						// 缩放图片比例
						opts.inSampleSize = com.csu.utils.Config.computeSampleSize(opts, -1, 400 * 400);
						opts.inJustDecodeBounds = false;
						watermarkBitmap = BitmapFactory.decodeStream(cr.openInputStream(uri), null, opts);

					} catch (FileNotFoundException e) {
						// Log.e("Exception", e.getMessage(),e);
					}
				}
				if (degree != 0) {//旋转
					Matrix matrix = new Matrix();
					matrix.setRotate(degree, watermarkBitmap.getWidth() / 2, watermarkBitmap.getHeight() / 2);
					watermarkBitmap = Bitmap.createBitmap(watermarkBitmap, 0, 0, watermarkBitmap.getWidth(), watermarkBitmap.getHeight(), matrix, true);
				}
				if(watermarkBitmap!=null&&dialog!=null){
					dialog.setPageImageBgBitmap(((MuPDFPageView)mDocView.getDisplayedView()).getPageBitmap());
					dialog.setWatermarkeImageBgBitmap(watermarkBitmap);
					dialog.show();
				}
			}
			break;
		case PICK_AUDIO:
			if(resultCode!=0)
			{
				Uri uri = data.getData();
				Log.d("weiquanyun","uri=="+uri);
				if(uri!=null){
					String []proj = {MediaStore.Audio.Media.DATA};
					Cursor cursor = null;
					try {
						cursor = managedQuery(uri, proj, null, null,null);
						int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
						cursor.moveToFirst();
						local_audio_path = cursor.getString(column_index);
					} catch (Exception e) {

					} finally {
						if (cursor != null && Integer.parseInt(Build.VERSION.SDK) < 14) {
							cursor.close();
						}
					}
				}
				if(local_audio_path!=null){
					int w = mDocView.getDisplayedView().getMeasuredWidth();
					int h = mDocView.getDisplayedView().getMeasuredHeight();
//					Log.d("weiquanyun","local_audio_path-->"+local_audio_path+", audioX = "+audioX+", audioY = "+audioY);
					String icon_path = FileUtils.APP_PATH+"/"+"sound.jpg";
					core.insertAudio(local_audio_path, icon_path.trim(), mDocView.getDisplayedViewIndex(), audioX, audioY,w,display.getHeight());
//					core.save();
				}
			}
			break;
		case RECORD_AUDIO:
		if(resultCode!=0){
				Uri uri = data.getData();
				if(uri!=null){
					Log.d("weiquanyun","record_audio_uri = "+uri);
					if(uri.toString().startsWith("file")){
						String path = uri.getEncodedPath();
						record_audio_path = Uri.decode(path);
						if(record_audio_path!=null){
							int w = mDocView.getDisplayedView().getMeasuredWidth();
							int h = mDocView.getDisplayedView().getMeasuredHeight();
							String icon_path = FileUtils.APP_PATH+"/"+"sound.jpg";
							core.insertAudio(record_audio_path, icon_path, mDocView.getDisplayedViewIndex(), audioX, audioY,w,display.getHeight());
//							core.save();
						}
						return;
					}
					Cursor cursor = null;
					String []proj = {MediaStore.Audio.Media.DATA};
					try {
						cursor = managedQuery(uri, proj, null, null,null);
						int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
						cursor.moveToFirst();
						record_audio_path = cursor.getColumnName(column_index);
						Log.d("weiquanyun","record_audio_path = "+record_audio_path);
					} catch (Exception e) {
						Log.d("weiquanyun",e.toString());
					} finally {
						if (cursor != null && Integer.parseInt(Build.VERSION.SDK) < 14) {
							cursor.close();
						}
					}
					if(record_audio_path!=null){
						int w = mDocView.getDisplayedView().getMeasuredWidth();
						int h = mDocView.getDisplayedView().getMeasuredHeight();
						String icon_path = FileUtils.APP_PATH+"/"+"sound.jpg";
						core.insertAudio(record_audio_path, icon_path, mDocView.getDisplayedViewIndex(), audioX, audioY,w,display.getHeight());
//						core.save();
					}
				}
			}
			
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public Object onRetainNonConfigurationInstance()
	{
		MuPDFCore mycore = core;
		core = null;
		return mycore;
	}

	private void reflowModeSet(boolean reflow)
	{
		mReflow = reflow;
		mDocView.setAdapter(mReflow ? new MuPDFReflowAdapter(this, core) : new MuPDFPageAdapter(this, core));
		mCopyTextButton.setColorFilter(mReflow ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
		setButtonEnabled(mAnnotButton, !reflow);
		setButtonEnabled(mSearchButton, !reflow);
		if (reflow) setLinkHighlight(false);
		setButtonEnabled(mLinkButton, !reflow);
		setButtonEnabled(mMoreButton, !reflow);
		mDocView.refresh(mReflow);
	}

	private void toggleReflow() {
		reflowModeSet(!mReflow);
		showInfo(mReflow ? getString(R.string.entering_reflow_mode) : getString(R.string.leaving_reflow_mode));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mFileName != null && mDocView != null) {
			outState.putString("FileName", mFileName);

			// Store current page in the prefs against the file name,
			// so that we can pick it up each time the file is loaded
			// Other info is needed only for screen-orientation change,
			// so it can go in the bundle
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
			edit.commit();
		}

		if (!mButtonsVisible)
			outState.putBoolean("ButtonsHidden", true);

		if (mTopBarMode == TopBarMode.Search)
			outState.putBoolean("SearchMode", true);

		if (mReflow)
			outState.putBoolean("ReflowMode", true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mSearchTask != null)
			mSearchTask.stop();

		if (mFileName != null && mDocView != null) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
			edit.commit();
		}
	}

	public void onDestroy()
	{
		if (core != null)
			core.onDestroy();
		if (mAlertTask != null) {
			mAlertTask.cancel(true);
			mAlertTask = null;
		}
		core = null;
//		File dir = new File(FileUtils.APP_PATH);
//		if(dir.exists())
//		{
//			FileUtils.getFileUtils().deleteDir(dir);
//		}
		super.onDestroy();
	}

	private void setButtonEnabled(ImageButton button, boolean enabled) {
		button.setEnabled(enabled);
		button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255):Color.argb(255, 128, 128, 128));
	}

	private void setLinkHighlight(boolean highlight) {
		mLinkHighlight = highlight;
		// LINK_COLOR tint
		mLinkButton.setColorFilter(highlight ? Color.argb(0xFF, 172, 114, 37) : Color.argb(0xFF, 255, 255, 255));
		// Inform pages of the change.
		mDocView.setLinksEnabled(highlight);
	}

	private void showButtons() {
		if (core == null)
			return;
		if (!mButtonsVisible) {
			mButtonsVisible = true;
			// Update page number text and slider
			int index = mDocView.getDisplayedViewIndex();
			updatePageNumView(index);
			mPageSlider.setMax((core.countPages()-1)*mPageSliderRes);
			mPageSlider.setProgress(index*mPageSliderRes);
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
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageSlider.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
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
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
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
				public void onAnimationRepeat(Animation animation) {}
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
		if (core == null)
			return;
		mPageNumberView.setText(String.format("%d / %d", index+1, core.countPages()));
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

		if (docUri.getScheme() == null)
			docUri = Uri.parse("file://"+docUri.toString());

		Intent printIntent = new Intent(this, PrintDialogActivity.class);
		printIntent.setDataAndType(docUri, "aplication/pdf");
		printIntent.putExtra("title", mFileName);
		startActivityForResult(printIntent, PRINT_REQUEST);
	}

	private void showInfo(String message) {
		mInfoView.setText(message);

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			SafeAnimatorInflater safe = new SafeAnimatorInflater((Activity)this, R.animator.info, (View)mInfoView);
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
		mButtonsView = getLayoutInflater().inflate(R.layout.buttons,null);
		mFilenameView = (TextView)mButtonsView.findViewById(R.id.docNameText);
		mPageSlider = (SeekBar)mButtonsView.findViewById(R.id.pageSlider);
		mPageNumberView = (TextView)mButtonsView.findViewById(R.id.pageNumber);
		mInfoView = (TextView)mButtonsView.findViewById(R.id.info);
		mSearchButton = (ImageButton)mButtonsView.findViewById(R.id.searchButton);
		mCopyTextButton = (ImageButton)mButtonsView.findViewById(R.id.copyTextButton);
		mOutlineButton = (ImageButton)mButtonsView.findViewById(R.id.outlineButton);
		mAnnotButton = (ImageButton)mButtonsView.findViewById(R.id.editAnnotButton);
		mAnnotTypeText = (TextView)mButtonsView.findViewById(R.id.annotType);
		mTopBarSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.switcher);
		mSearchBack = (ImageButton)mButtonsView.findViewById(R.id.searchBack);
		mSearchFwd = (ImageButton)mButtonsView.findViewById(R.id.searchForward);
		mSearchText = (EditText)mButtonsView.findViewById(R.id.searchText);
		mLinkButton = (ImageButton)mButtonsView.findViewById(R.id.linkButton);
		mMoreButton = (ImageButton)mButtonsView.findViewById(R.id.moreButton);
		mTopBarSwitcher.setVisibility(View.INVISIBLE);
		mPageNumberView.setVisibility(View.INVISIBLE);
		mInfoView.setVisibility(View.INVISIBLE);
		mPageSlider.setVisibility(View.INVISIBLE);
	}

	public void OnMoreButtonClick(View v) {
//		mTopBarMode = TopBarMode.More;
//		mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		mTopBarMode = TopBarMode.Annot;
		mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		//在用户选择编辑注释模式中，长按无效
		mDocView.setAnnoMode(true);
	}

	public void OnCancelMoreButtonClick(View v) {
		mTopBarMode = TopBarMode.Main;
		mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		//恢复长按有效
		mDocView.setAnnoMode(false);
	}

	public void OnPrintButtonClick(View v) {
		printDoc();
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
			mTopBarMode = TopBarMode.Main;
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
			if (pageView != null)
				success = pageView.copySelection();
			mTopBarMode = TopBarMode.Main;
			showInfo(success?getString(R.string.copied_to_clipboard):getString(R.string.no_text_selected));
			break;

		case Highlight:
			if (pageView != null)
				success = pageView.markupSelection(Annotation.Type.HIGHLIGHT);
			mTopBarMode = TopBarMode.Annot;
			if (!success)
				showInfo(getString(R.string.no_text_selected));
			break;

		case Underline:
			if (pageView != null)
				success = pageView.markupSelection(Annotation.Type.UNDERLINE);
			mTopBarMode = TopBarMode.Annot;
			if (!success)
				showInfo(getString(R.string.no_text_selected));
			break;

		case StrikeOut:
			if (pageView != null)
				success = pageView.markupSelection(Annotation.Type.STRIKEOUT);
			mTopBarMode = TopBarMode.Annot;
			if (!success)
				showInfo(getString(R.string.no_text_selected));
			break;

		case Ink:
			if (pageView != null)
				success = pageView.saveDraw();
			mTopBarMode = TopBarMode.Annot;
			if (!success)
				showInfo(getString(R.string.nothing_to_save));
			break;
		}
		mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		mDocView.setMode(MuPDFReaderView.Mode.Viewing);
	}

	public void OnCancelSearchButtonClick(View v) {
		searchModeOff();
	}
	
	/**
	 * delete annotation
	 * @param v
	 */
	public void OnDeleteButtonClick(View v) {
		MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
		if (pageView != null)
			pageView.deleteSelectedAnnotation();
		mTopBarMode = TopBarMode.Annot;
		mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
	}
	/**
	 * cancel delete annotation
	 * @param v
	 */
	public void OnCancelDeleteButtonClick(View v) {
		MuPDFView pageView = (MuPDFView) mDocView.getDisplayedView();
		if (pageView != null)
			pageView.deselectAnnotation();
		mTopBarMode = TopBarMode.Annot;
		mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.showSoftInput(mSearchText, 0);
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
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
//		if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
//			hideButtons();
//		} else {
//			showButtons();
//			searchModeOff();
//		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		setIconEnable(menu, true);
		//groupId itemId order title
		SubMenu subMenuOfTool = menu.addSubMenu(Menu.NONE, 1, 1, R.string.option_menu_tool).setIcon(R.drawable.ic_tool);
		menu.addSubMenu(Menu.NONE, 2, 2, R.string.option_menu_help).setIcon(R.drawable.ic_help);
		int base = Menu.FIRST;
		subMenuOfTool.add(base,11,base+1,R.string.annotation);
//		subMenuOfTool.add(base, 12, base+2, R.string.insert_audio);
		subMenuOfTool.add(base, 13, base+3, R.string.insert_watermark);
//		if(core.isPageHasWatermark(mDocView.getDisplayedViewIndex())==1)
//			subMenuOfTool.add(base, 14, base+4, R.string.delete_watermark);
		subMenuOfTool.setHeaderTitle(R.string.option_tools);
		subMenuOfTool.setHeaderIcon(R.drawable.ic_tool);
		return true;
	}
	//通过java反射机制调用MenuBuilder的方法
	private void setIconEnable(Menu menu, boolean enable)  {  
		try   
		{  
			Class<?> clazz = Class.forName("com.android.internal.view.menu.MenuBuilder");  
			Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);  
			m.setAccessible(true);  

			//MenuBuilder实现Menu接口，创建菜单时，传进来的menu其实就是MenuBuilder对象(java的多态特征)  
			m.invoke(menu, enable);  

		} catch (Exception e)   
		{  
			e.printStackTrace();  
		}  
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Menu.FIRST:
        	break;
        case Menu.FIRST+1:
			Uri docUri = Uri.parse("/storage/sdcard0/360Download/Quick Start Guide.pdf");
			Intent intent = new Intent(MuPDFActivity.this,MuPDFActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(docUri);
			startActivity(intent); 
        	Toast.makeText(MuPDFActivity.this, "help", Toast.LENGTH_SHORT).show();
        	break;
        case 11:
        	mTopBarMode = TopBarMode.Annot;
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        	Toast.makeText(MuPDFActivity.this, "annotation", Toast.LENGTH_SHORT).show();
        	break;
        case 12:
        	Toast.makeText(MuPDFActivity.this, "audio", Toast.LENGTH_SHORT).show();
        	break;
        case 13:
        	if(core.isPageHasWatermark(mDocView.getDisplayedViewIndex())==1){
        		Toast.makeText(MuPDFActivity.this, R.string.page_has_watermark, Toast.LENGTH_SHORT).show();
        	}else{
        		insertImageWatermark();
        	}
        	break;
        case 14:
        	Toast.makeText(MuPDFActivity.this, R.string.delete_watermark, Toast.LENGTH_SHORT).show();
        	break;
        }
        return false;
	}
	
	//insert audio
	private void insertMediaAudio(int x,int y){
		audioX = x;
		audioY = y;
		int w = mDocView.getDisplayedView().getMeasuredWidth();
		int h = mDocView.getDisplayedView().getMeasuredHeight();
		Log.d("weiquanyun", "x = "+x+" ,y = "+y+", w = "+w+", h = "+h);
		File iconFileDir = new File(FileUtils.APP_PATH);
		if (!iconFileDir.exists()) {
			iconFileDir.mkdirs();
		}
		File file = new File(iconFileDir, "sound.jpg");//保icon图表
		if (!file.exists()) {
			//把Asset下的文件copy到sdcard
			Log.d("weiquanyun","file not exists");
			file = com.csu.utils.FileUtils.getAssetFileToFileDir(iconFileDir,MuPDFActivity.this, "sound.jpg");
		}
		sound_icon_path = file.getAbsolutePath(); 
		AlertDialog.Builder builder = new AlertDialog.Builder(MuPDFActivity.this);
		builder.setTitle("选择音频");
		builder.setPositiveButton(R.string.record_audio, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setType("audio/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, RECORD_AUDIO);
				insertAudioDialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.local_audio, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setType("audio/mp3");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, PICK_AUDIO);
				insertAudioDialog.dismiss();
			}
		});
		insertAudioDialog = builder.create();
		insertAudioDialog.show();
	}
	
	//insert watermark
	private void insertImageWatermark(){
		
		int screenWidth;
		int screenHeight;
		int dialogWidth;
		int dialogHeight;
		int pageWidth;
		int pageHeight;
		int w = mDocView.getDisplayedView().getMeasuredWidth();
		int h = mDocView.getDisplayedView().getMeasuredHeight();
		if(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
			dialogWidth = screenWidth/3;
			dialogHeight = screenHeight*3/4;
		}else{
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
			dialogWidth = screenWidth*3/4;
			dialogHeight = screenHeight/2+30;
		}
		pageWidth = (int)(dialogWidth*0.6);
		pageHeight = (int)(pageWidth*((float)h/w));
		dialog = new InsertWatermarkDialog(MuPDFActivity.this, dialogHeight, dialogWidth, pageHeight, pageWidth);
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, PHOTO_PICKED_DATA);
		dialog.getCancelButton().setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
			
		});
		dialog.getSureButton().setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				int w = mDocView.getDisplayedView().getMeasuredWidth();
				int h = mDocView.getDisplayedView().getMeasuredHeight();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				//original watermark size
				int newBitmapWidth = watermarkBitmap.getWidth();
				int newBitmapHeight = watermarkBitmap.getHeight();
				//new watermark size
				int watermarkWidth = 0;
				int watermarkHeight = 0;
				if(dialog.getWatermarkSize()==InsertWatermarkDialog.WatermarkSize.SMALL)
					watermarkBitmap = Bitmap.createScaledBitmap(watermarkBitmap, newBitmapWidth/2, newBitmapHeight/2, false);
				else if(dialog.getWatermarkSize()==InsertWatermarkDialog.WatermarkSize.LARGE){
					if(newBitmapWidth>newBitmapHeight){
						if(newBitmapWidth>w*2/3){
							watermarkWidth = w;
							watermarkHeight = w/newBitmapWidth*newBitmapHeight;
							watermarkBitmap = Bitmap.createScaledBitmap(watermarkBitmap, watermarkWidth, watermarkHeight, false);
						}
						if(newBitmapWidth<w*2/3){
							watermarkWidth = w*2/3;
							watermarkHeight = w/newBitmapWidth*newBitmapHeight;
							watermarkBitmap = Bitmap.createScaledBitmap(watermarkBitmap, watermarkWidth, watermarkHeight, false);
						}
					}else{
						if(newBitmapHeight>w*2/3){
							watermarkHeight = h;
							watermarkWidth = h/newBitmapHeight*newBitmapWidth;
							watermarkBitmap = Bitmap.createScaledBitmap(watermarkBitmap, watermarkWidth, watermarkHeight, false);
						}else{
							watermarkHeight = h*2/3;
							watermarkWidth = h/newBitmapHeight*newBitmapWidth;
							watermarkBitmap = Bitmap.createScaledBitmap(watermarkBitmap, watermarkWidth, watermarkHeight, false);
						}
					}
				}
				watermarkBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				byte[] data = baos.toByteArray();
				Log.d("weiquanyun","watermarkBitmap.getWidth() = "+watermarkBitmap.getWidth()+" watermarkBitmap.getHeight() = "+watermarkBitmap.getHeight()+" data.length = "+data.length);
				core.addWatermarkWithImage(mDocView.getDisplayedViewIndex(), data, watermarkBitmap.getWidth(), watermarkBitmap.getHeight(),w,h,data.length);
				core.save();
				dialog.dismiss();
				watermarkBitmap.recycle();
				watermarkBitmap = null;
			}
			
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (core != null)
		{
			core.startAlerts();
			createAlertWaiter();
		}
		MuPDFReaderView.InsertAudioCallback mInsertAudioCallback = new MuPDFReaderView.InsertAudioCallback(){
			public void insertAudio(int x,int y){
				insertMediaAudio(x,y);
			} 
		};
		if(mDocView!=null)
		{
			mDocView.setInsertAudioCallback(mInsertAudioCallback);
		}
	}

	@Override
	protected void onStop() {
		if (core != null)
		{
			destroyAlertWaiter();
			core.stopAlerts();
		}

		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (core.hasChanges()) {
			DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (which == AlertDialog.BUTTON_POSITIVE)
						core.save();

					finish();
				}
			};
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle(getString(R.string.app_name));
			alert.setMessage(getString(R.string.document_has_changes_save_them_));
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
			alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
			alert.show();
		} else {
			super.onBackPressed();
		}
	}
}
