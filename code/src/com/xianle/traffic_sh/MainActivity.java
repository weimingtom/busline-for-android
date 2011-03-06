package com.xianle.traffic_sh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {

	private ArrayList<String> directoryEntries = new ArrayList<String>();
	TextView tv;
	private DataDownloader downloader = null;
	private File currentDirectory;
	private ProgressDialog mDialog;
	
	// because the zip lib doesn't support chinese file name 
	// so, we have to define a hashtable to map english file name to chinese file name 
	final  Hashtable<String, String> mFileName = new Hashtable<String, String>();
	final  ArrayList<String> mChineseFileNameList = new ArrayList<String>(4);
	
	
	private void initFileNameMap() {
		mFileName.put("shanghai", this.getResources().getString(R.string.shanghai));
		mFileName.put("beijing", this.getResources().getString(R.string.beijing));
		mFileName.put("guangzhou", this.getResources().getString(R.string.guangzhou));
		mFileName.put("shenzhen", this.getResources().getString(R.string.shenzhen));
		mFileName.put("chengdu", this.getResources().getString(R.string.chengdu));
		mFileName.put("fuzhou", this.getResources().getString(R.string.fuzhou));
		mFileName.put("hefei", this.getResources().getString(R.string.hefei));
		mFileName.put("wuhan", this.getResources().getString(R.string.wuhan));
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		initFileNameMap();
		unzip();
		// browseToRoot();
	}

	protected void OnPause() {
		super.onPause();
		if (downloader != null) {
			synchronized (downloader) {
				downloader.setStatusField(null);
			}
		}
	}

	protected void OnResume() {
		super.onResume();
		if (downloader != null) {
			synchronized (downloader) {
				downloader.setStatusField(tv);
				// if( downloader.DownloadComplete )
				// initSDL();
			}
		}
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	boolean result = true;
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if(currentDirectory.getName().equals("busline")) {
    			finish();
    		} else {
    			browseTo(currentDirectory.getParentFile(),0);
    		}
    	} 
        return result;
    }
	

	/**
	 * move the file in asset to directory /sdcard/busline
	 */
	private void unzip() {
		Log.v(Globals.TAG, "start unzip file or download file");
		tv = new TextView(this);
		class CallBack implements Runnable {
			public MainActivity mParent;

			public void run() {
				if (mParent.downloader == null)
					mParent.downloader = new DataDownloader(mParent, tv);
			}
		}
		CallBack cb = new CallBack();
		cb.mParent = this;
		this.runOnUiThread(cb);
		 mDialog = CreateDialog();
		mDialog.show();
	}
	
	protected ProgressDialog CreateDialog() {
		ProgressDialog dialog = new ProgressDialog(this);
		dialog
				.setMessage(this.getResources().getString(
						R.string.pregress_diag));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		return dialog;
	}

	private void browseTo(final File aDirectory, final long id) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());
		} else {
			DialogInterface.OnClickListener okButtonListener = new DialogInterface.OnClickListener() {
				// @Override
				public void onClick(DialogInterface arg0, int arg1) {
					try {
						InputStream checkFile = null;
						try {
							Intent in = new Intent(MainActivity.this,
									Traffic.class);
							in.putExtra(Globals.FILENAME, aDirectory.getPath());
							in.putExtra(Globals.Title, mChineseFileNameList.get((int)id));
							MainActivity.this.startActivity(in);
						} catch (Exception e) {
							Context context = getApplicationContext();
							CharSequence text = MainActivity.this
									.getResources().getString(
											R.string.diag_err);
							int duration = Toast.LENGTH_SHORT;

							Toast toast = Toast.makeText(context, text,
									duration);
							toast.show();
						}
						;

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			DialogInterface.OnClickListener cancelButtonListener = new DialogInterface.OnClickListener() {
				// @Override
				public void onClick(DialogInterface dialog, int which) {

					dialog.dismiss();
				}
			};
			AlertDialog ad = new AlertDialog.Builder(this).setMessage(
					R.string.diag_msg).setPositiveButton(android.R.string.ok,
					okButtonListener).setNegativeButton(
					android.R.string.cancel, cancelButtonListener).create();
			ad.show();
		}
	}

	private void fill(File[] files) {
		this.directoryEntries.clear();
		this.mChineseFileNameList.clear();
		
		for (File file : files) {
			if (!file.getName().endsWith(".txt") && !file.isDirectory())
				continue;
			final String name;
			if (!file.isDirectory()) {
				name = file.getName().substring(0,
						file.getName().lastIndexOf('.'));
			} else {
				name = file.getName();
			}
			this.directoryEntries.add(name);
		}

		Comparator<String> cmp = new ChinsesCharComp();
		Collections.sort(directoryEntries, cmp);
		Iterator<String> it = directoryEntries.iterator();
		while(it.hasNext()) {
			final String englishFileName = it.next();
			final String chineseFileName = mFileName.get(englishFileName);
			
			if (chineseFileName != null) {
				mChineseFileNameList.add(chineseFileName);
			} else {
				mChineseFileNameList.add(englishFileName);
			}
			
		}
		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mChineseFileNameList);

		this.setListAdapter(directoryList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		File clickedFile = null;
		clickedFile = new File(this.currentDirectory.getAbsolutePath()
				+ File.separator + this.directoryEntries.get(position));
		if(clickedFile.isDirectory()) {
			Log.v(Globals.TAG, "is a directory");
			this.browseTo(clickedFile, id);
			return;
		} 
		clickedFile = new File(this.currentDirectory.getAbsolutePath()
				+ File.separator + this.directoryEntries.get(position) + ".txt");
		
		try {
			if (clickedFile != null && clickedFile.isFile())
				this.browseTo(clickedFile, id);
				
		} catch (Exception e) {
			//don't throw
		}
		
	}

	class ChinsesCharComp implements Comparator<String> {

		public int compare(String o1, String o2) {
			String c1 = (String) o1;
			String c2 = (String) o2;
			Collator myCollator = Collator.getInstance(java.util.Locale.CHINA);
			if (myCollator.compare(c1, c2) < 0)
				return -1;
			else if (myCollator.compare(c1, c2) > 0)
				return 1;
			else
				return 0;
		}
	}

	public void getFileList() {
		mDialog.dismiss();
		browseTo(new File(Globals.DataDir),0);
	}
}
