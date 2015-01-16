package com.csu.powerpdf;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.csu.utils.Config;
import com.csu.utils.IconContextMenu;
import com.csu.utils.IconContextMenu.IconContextMenuOnClickListener;

public class MuPDFReaderView extends ReaderView {
	enum Mode {Viewing, Selecting, Drawing}
	private final Context mContext;
	private boolean mLinksEnabled = false;
	private Mode mMode = Mode.Viewing;
	private boolean tapDisabled = false;
	private int tapPageMargin;

	protected void onTapMainDocArea() {}
	protected void onDocMotion() {}
	protected void onHit(Hit item) {};

	//长按弹出的菜单
	private IconContextMenu contextMenu;
	private Activity parentActivity;
	//context menu
	private boolean isLongPress = false;
	private RelativeLayout contextMenuLayout;
	private LayoutInflater inflater;
	private PopupWindow contextMenuWindow;
	private TextView selectText;
	private TextView insertAudio;
	//菜单ID
	Dialog menu;
	private final int DIALOG_ID = 1111;
	final int HIGHLIGHT_TEXT = 0;
	final int UNDERLINE_TEXT = 1;
	final int DELETE_TEXT = 2;
	boolean menuClickFlag = false;
	//判断是否是annotation模式
	private boolean annoMode = false;
	
	public void setAnnoMode(boolean annoMode){
		this.annoMode = annoMode;
	}
	
	//定义音频插入回调
	private InsertAudioCallback mInsertAudioCallback;
	public void setInsertAudioCallback(InsertAudioCallback mInsertAudioCallback){
		this.mInsertAudioCallback = mInsertAudioCallback;
	}
	
	//insert audio position
	int audioX = 0;
	int audioY = 0;
	static class InsertAudioCallback{
		public void insertAudio(int x,int y){};
	}
	public void setLinksEnabled(boolean b) {
		mLinksEnabled = b;
		resetupChildren();
	}

	public void setMode(Mode m) {
		mMode = m;
	}

	public MuPDFReaderView(Activity act) {
		super(act);
		mContext = act;
		parentActivity = act;
		inflater = act.getLayoutInflater();
		contextMenuLayout = (RelativeLayout)inflater.inflate(R.layout.context_menu_1, null).findViewById(R.id.context_menu_first);
		Resources res = parentActivity.getResources();
		contextMenu = new IconContextMenu(DIALOG_ID, parentActivity);
		contextMenu.addItem(res, R.string.highlight, R.drawable.ic_highlight, HIGHLIGHT_TEXT);
		contextMenu.addItem(res, R.string.underline, R.drawable.ic_underline, UNDERLINE_TEXT);
		contextMenu.addItem(res, R.string.delete, R.drawable.ic_strike, DELETE_TEXT);
		// Get the screen size etc to customise tap margins.
		// We calculate the size of 1 inch of the screen for tapping.
		// On some devices the dpi values returned are wrong, so we
		// sanity check it: we first restrict it so that we are never
		// less than 100 pixels (the smallest Android device screen
		// dimension I've seen is 480 pixels or so). Then we check
		// to ensure we are never more than 1/5 of the screen width.
		DisplayMetrics dm = new DisplayMetrics();
		act.getWindowManager().getDefaultDisplay().getMetrics(dm);
		tapPageMargin = (int)dm.xdpi;
		if (tapPageMargin < 100)
			tapPageMargin = 100;
		if (tapPageMargin > dm.widthPixels/5)
			tapPageMargin = dm.widthPixels/5;
	}

	public boolean onSingleTapUp(MotionEvent e) {
		LinkInfo link = null;

		if (mMode == Mode.Viewing && !tapDisabled) {
			MuPDFView pageView = (MuPDFView) getDisplayedView();
			Hit item = pageView.passClickEvent(e.getX(), e.getY());
			onHit(item);
			if (item == Hit.Nothing) {
				if (mLinksEnabled && pageView != null
				&& (link = pageView.hitLink(e.getX(), e.getY())) != null) {
					link.acceptVisitor(new LinkInfoVisitor() {
						@Override
						public void visitInternal(LinkInfoInternal li) {
							// Clicked on an internal (GoTo) link
							setDisplayedViewIndex(li.pageNumber);
						}

						@Override
						public void visitExternal(LinkInfoExternal li) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(li.url));
							mContext.startActivity(intent);
						}

						@Override
						public void visitRemote(LinkInfoRemote li) {
							// Clicked on a remote (GoToR) link
						}
					});
				} else if (e.getX() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getX() > super.getWidth() - tapPageMargin) {
					super.smartMoveForwards();
				} else if (e.getY() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getY() > super.getHeight() - tapPageMargin) {
					super.smartMoveForwards();
				} else {
					onTapMainDocArea();
				}
			}
		}
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {

		return super.onDown(e);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		MuPDFView pageView = (MuPDFView)getDisplayedView();
		switch (mMode) {
		case Viewing:
			if (!tapDisabled)
				onDocMotion();

			return super.onScroll(e1, e2, distanceX, distanceY);
		case Selecting:
			if (pageView != null)
				pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
			return true;
		default:
			return true;
		}
	}

	//增加长按事件，可以弹出菜单
	@Override
	public void onLongPress(MotionEvent e) {
		if(annoMode||getScale()>1.0f)
			return;
		Log.d("weiquanyun","page.left"+((MuPDFPageView)getDisplayedView()).getLeft()+"page.top"+((MuPDFPageView)getDisplayedView()).getTop());
		int left = ((MuPDFPageView)getDisplayedView()).getLeft();
		int top = ((MuPDFPageView)getDisplayedView()).getTop();
		int width = ((MuPDFPageView)getDisplayedView()).getMeasuredWidth();
		int height = ((MuPDFPageView)getDisplayedView()).getMeasuredHeight();
		if(e.getX()<left||e.getY()<top)
			return;
		if(e.getX()-left>width||e.getY()-top>height)
			return;
		isLongPress = true;
		HorizontalScrollView scrollView = (HorizontalScrollView)contextMenuLayout.findViewById(R.id.menu_first_hscroll);
		contextMenuWindow = new PopupWindow(mContext);
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)scrollView.getLayoutParams();
		params.setMargins(10, 2, 10, 0);
		contextMenuWindow.setContentView(contextMenuLayout);
		Paint paint = new Paint();
		paint.setTextSize(Config.dp2pxF(mContext, 18));
		int contextMenuWIndowWidth = (int)paint.measureText("选择文本")*2+10;
		if(contextMenuWIndowWidth>width)
			contextMenuWIndowWidth = width*2/3;
		contextMenuWindow.setWidth(contextMenuWIndowWidth);
		contextMenuWindow.setHeight(120);
		contextMenuWindow.setBackgroundDrawable(new BitmapDrawable());
		contextMenuWindow.setOutsideTouchable(true);
		contextMenuWindow.setFocusable(true);
		int anchorX = (int)e.getX();
		int anchorY = (int)e.getY();
		int contextMenuWidth = contextMenuWindow.getWidth();
		int contextMenuHeight = contextMenuWindow.getHeight();
		if(anchorX>contextMenuWidth/2&&anchorY>contextMenuHeight){
			contextMenuWindow.showAtLocation(getDisplayedView(), Gravity.NO_GRAVITY, anchorX-contextMenuWidth/2, anchorY-contextMenuHeight);
		}else if(anchorX>contextMenuWidth/2){
			contextMenuWindow.showAtLocation(getDisplayedView(), Gravity.NO_GRAVITY, anchorX-contextMenuWidth/2, anchorY);
		}else if(anchorY>contextMenuHeight){
			contextMenuWindow.showAtLocation(getDisplayedView(), Gravity.NO_GRAVITY, 0, anchorY-contextMenuHeight);
		}else{
			contextMenuWindow.showAtLocation(getDisplayedView(), Gravity.NO_GRAVITY, 0, anchorY);
		}
		selectText = (TextView)contextMenuLayout.findViewById(R.id.menu_first_select_text);
		insertAudio = (TextView)contextMenuLayout.findViewById(R.id.menu_first_insert_audio);
		
		selectText.setOnClickListener(new MenuSelectClickListener());
		audioX = (int)e.getX();
		audioY = (int)e.getY();
		insertAudio.setOnClickListener(new MenuSelectClickListener());
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		switch (mMode) {
		case Viewing:
			return super.onFling(e1, e2, velocityX, velocityY);
		default:
			return true;
		}
	}

	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		Log.d("weiquanyun","onScaleBegin----");
		tapDisabled = true;
		return super.onScaleBegin(d);
	}

	public boolean onTouchEvent(MotionEvent event) {

		if ( mMode == Mode.Drawing )
		{
			float x = event.getX();
			float y = event.getY();
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					touch_start(x, y);
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move(x, y);
					break;
				case MotionEvent.ACTION_UP:
					touch_up();
					break;
			}
		}
		
		if ( mMode == Mode.Selecting&&isLongPress )
		{
			switch (event.getAction())
			{
				case MotionEvent.ACTION_UP:
					touch_up();
					break;
			}
		}

		if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN)
		{
			tapDisabled = false;
		}

		return super.onTouchEvent(event);
	}

	private float mX, mY;

	private static final float TOUCH_TOLERANCE = 2;

	private void touch_start(float x, float y) {

		MuPDFView pageView = (MuPDFView)getDisplayedView();
		if (pageView != null)
		{
			pageView.startDraw(x, y);
		}
		mX = x;
		mY = y;
	}

	private void touch_move(float x, float y) {

		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
		{
			MuPDFView pageView = (MuPDFView)getDisplayedView();
			if (pageView != null)
			{
				pageView.continueDraw(x, y);
			}
			mX = x;
			mY = y;
		}
	}

	private void touch_up() {
		// NOOP
		if(this.mMode.equals(MuPDFReaderView.Mode.Selecting)){
			menu = contextMenu.createMenu(mContext.getString(R.string.text_oper));
			menu.show();
			menu.setOnDismissListener(new OnDismissListener(){

				@Override
				public void onDismiss(DialogInterface dialog) {
					// TODO Auto-generated method stub
					if(!menuClickFlag){
						MuPDFView pageView = (MuPDFView)getDisplayedView();
						if(pageView!=null){
							Log.d("weiquanyun","menu  dismiss");
							pageView.deselectText();
							setMode(MuPDFReaderView.Mode.Viewing);
							isLongPress = false;
						}
					}
						
				}
				
			});
			menuClickFlag = false;
			contextMenu.setOnClick(new IconContextMenuOnClickListener() {
				
				@Override
				public void onClick(int menuId) {
					// TODO Auto-generated method stub
					switch(menuId){
					case 0:
						onHighlightMenuClick();
						menuClickFlag = true;
						break;
					case 1:
						onUnderlineMenuClick();
						menuClickFlag = true;
						break;
					case 2:
						onDeleteMenuClick();
						menuClickFlag = true;
						break;
					}
					menu.dismiss();
					isLongPress = false;
				}
			});
		}
	}
	//set text highlight
	private void onHighlightMenuClick(){
		((MuPDFView) getDisplayedView()).markupSelection(Annotation.Type.HIGHLIGHT);
		setMode(MuPDFReaderView.Mode.Viewing);
	}
	//set text underline
	private void onUnderlineMenuClick(){
		((MuPDFView) getDisplayedView()).markupSelection(Annotation.Type.UNDERLINE);
		setMode(MuPDFReaderView.Mode.Viewing);
	}
	// delete
	private void onDeleteMenuClick(){
		((MuPDFView) getDisplayedView()).markupSelection(Annotation.Type.STRIKEOUT);
		setMode(MuPDFReaderView.Mode.Viewing);
	}
	protected void onChildSetup(int i, View v) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber == i)
			((MuPDFView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		else
			((MuPDFView) v).setSearchBoxes(null);

		((MuPDFView) v).setLinkHighlighting(mLinksEnabled);

		((MuPDFView) v).setChangeReporter(new Runnable() {
			public void run() {
				applyToChildren(new ReaderView.ViewMapper() {
					@Override
					void applyToView(View view) {
						((MuPDFView) view).update();
					}
				});
			}
		});
	}

	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
	}

	@Override
	protected void onMoveOffChild(int i) {
		View v = getView(i);
		if (v != null)
			((MuPDFView)v).deselectAnnotation();
	}

	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
		((MuPDFView) v).addHq(false);
	}

	protected void onUnsettle(View v) {
		// When something changes making the previous settled view
		// no longer appropriate, tell the page to remove HQ
		((MuPDFView) v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((MuPDFView) v).releaseResources();
	}

	@Override
	protected void onScaleChild(View v, Float scale) {
		((MuPDFView) v).setScale(scale);
	}
	
	class MenuSelectClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(v.equals(selectText)){
				setMode(MuPDFReaderView.Mode.Selecting);
			}else if(v.equals(insertAudio)){
				mInsertAudioCallback.insertAudio(audioX,audioY);
			}
			contextMenuWindow.dismiss();
		}
		
	}
}
