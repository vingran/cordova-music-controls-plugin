package com.homerours.musiccontrols;

import org.apache.cordova.CordovaInterface;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.UUID;

import android.util.Log;
import android.R;
import android.content.Context;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.net.Uri;
import android.media.session.MediaSession.Token;

import android.app.NotificationChannel;

public class MusicControlsNotification {
	private Activity cordovaActivity;
	private NotificationManager notificationManager;
	private Notification.Builder notificationBuilder;
	private int notificationID;
	protected MusicControlsInfos infos;
	private Bitmap bitmapCover;
	private String CHANNEL_ID;
	private Token token;
	private static final String TAG = "MusicControlsNotif";
	
	// Codes uniques pour chaque PendingIntent
	private static final int PENDING_INTENT_PREVIOUS = 1;
	private static final int PENDING_INTENT_PLAY = 2;
	private static final int PENDING_INTENT_PAUSE = 3;
	private static final int PENDING_INTENT_NEXT = 4;
	private static final int PENDING_INTENT_CLOSE = 5;
	private static final int PENDING_INTENT_DISMISS = 6;
	private static final int PENDING_INTENT_CONTENT = 7;

	// Public Constructor
	public MusicControlsNotification(Activity cordovaActivity, int id, Token token){
		this.CHANNEL_ID = UUID.randomUUID().toString();
		this.notificationID = id;
		this.cordovaActivity = cordovaActivity;
		Context context = cordovaActivity;
		this.token = token;
		this.notificationManager = (NotificationManager) cordovaActivity.getSystemService(Context.NOTIFICATION_SERVICE);

		// Log.d(TAG, "Constructor called with token: " + (token != null ? "NOT NULL" : "NULL"));

		// use channelID for Oreo and higher
		if (Build.VERSION.SDK_INT >= 26) {
			// Log.d(TAG, "Creating notification channel for API " + Build.VERSION.SDK_INT);
			// The user-visible name of the channel.
			CharSequence name = "Audio Controls";
			// The user-visible description of the channel.
			String description = "Control Playing Audio";

			// CORRECTION ANDROID 16: Changez IMPORTANCE_LOW à IMPORTANCE_DEFAULT
			int importance = NotificationManager.IMPORTANCE_LOW;

			NotificationChannel mChannel = new NotificationChannel(this.CHANNEL_ID, name, importance);

			// Configure the notification channel.
			mChannel.setDescription(description);

			// Don't show badges for this channel
			mChannel.setShowBadge(false);

			// CORRECTION ANDROID 16: Autorisez les bulles (bubbles) pour Android 14+
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				// Log.d(TAG, "Setting allowBubbles for Android 14+");
				mChannel.setAllowBubbles(true);
			}

			this.notificationManager.createNotificationChannel(mChannel);
			// Log.d(TAG, "Notification channel created with ID: " + this.CHANNEL_ID);
    	}
	}

	// Show or update notification
	public void updateNotification(MusicControlsInfos newInfos){
		// Log.d(TAG, "updateNotification called");
		// Log.d(TAG, "Track: " + newInfos.track + ", Artist: " + newInfos.artist);
		
		int nbTry = 0;
		//Add try/catch to avoid app crashed, and try four times
		do {
			try {
				// Check if the cover has changed	
				if (!newInfos.cover.isEmpty() && (this.infos == null || !newInfos.cover.equals(this.infos.cover))){
					// Log.d(TAG, "Getting bitmap cover: " + newInfos.cover);
					this.getBitmapCover(newInfos.cover);
				}
				this.infos = newInfos;
				this.createBuilder();
				Notification noti = this.notificationBuilder.build();
				// Log.d(TAG, "Notification built successfully");
				
				// Vérifier que la notification a bien les actions
				if (noti.actions != null) {
					// Log.d(TAG, "Notification has " + noti.actions.length + " actions");
				} else {
					// Log.d(TAG, "WARNING: Notification has NO actions");
				}
				
				this.notificationManager.notify(this.notificationID, noti);
				// Log.d(TAG, "Notification displayed with ID: " + this.notificationID);
				
				this.onNotificationUpdated(noti);
				nbTry = 10;
			} catch (Exception e) {
				// Log.e(TAG, "Error in updateNotification (attempt " + nbTry + ")", e);
				this.destroy();
				nbTry++;
			}
		} while (nbTry <= 3);
	}

	// Toggle the play/pause button
	public void updateIsPlaying(boolean isPlaying){
		// Log.d(TAG, "updateIsPlaying called: " + isPlaying);
		this.infos.isPlaying=isPlaying;
		this.createBuilder();
		Notification noti = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, noti);
		this.onNotificationUpdated(noti);
	}

	// Toggle the dismissable status
	public void updateDismissable(boolean dismissable){
		// Log.d(TAG, "updateDismissable called: " + dismissable);
		this.infos.dismissable=dismissable;
		this.createBuilder();
		Notification noti = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, noti);
		this.onNotificationUpdated(noti);
	}

	// Get image from url
	private void getBitmapCover(String coverURL){
		try{
			if(coverURL.matches("^(https?|ftp)://.*$"))
				// Remote image
				this.bitmapCover = getBitmapFromURL(coverURL);
			else{
				// Local image
				this.bitmapCover = getBitmapFromLocal(coverURL);
			}
		} catch (Exception ex) {
			// Log.e(TAG, "Error getting bitmap cover", ex);
			ex.printStackTrace();
		}
	}

	// get Local image
	private Bitmap getBitmapFromLocal(String localURL){
		try {
			Uri uri = Uri.parse(localURL);
			File file = new File(uri.getPath());
			FileInputStream fileStream = new FileInputStream(file);
			BufferedInputStream buf = new BufferedInputStream(fileStream);
			Bitmap myBitmap = BitmapFactory.decodeStream(buf);
			buf.close();
			return myBitmap;
		} catch (Exception ex) {
			try {
				InputStream fileStream = cordovaActivity.getAssets().open("www/" + localURL);
				BufferedInputStream buf = new BufferedInputStream(fileStream);
				Bitmap myBitmap = BitmapFactory.decodeStream(buf);
				buf.close();
				return myBitmap;
			} catch (Exception ex2) {
				ex.printStackTrace();
				ex2.printStackTrace();
				return null;
			}
		}
	}

	// get Remote image
	private Bitmap getBitmapFromURL(String strURL) {
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			return BitmapFactory.decodeStream(input);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private void createBuilder(){
		// Log.d(TAG, "createBuilder called");
		Context context = cordovaActivity;
		Notification.Builder builder = new Notification.Builder(context);

		// use channelID for Oreo and higher
		if (Build.VERSION.SDK_INT >= 26) {
			builder.setChannelId(this.CHANNEL_ID);
			// Log.d(TAG, "Channel ID set: " + this.CHANNEL_ID);
		}

		//Configure builder
		builder.setContentTitle(infos.track);
		if (!infos.artist.isEmpty()){
			builder.setContentText(infos.artist);
		}
		builder.setWhen(0);
		// Log.d(TAG, "Content title/text set");

		// set if the notification can be destroyed by swiping
		if (infos.dismissable){
			builder.setOngoing(false);
			Intent dismissIntent = new Intent("music-controls-destroy");
			// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire pour les intents implicites
			int dismissFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_DISMISS, dismissIntent, dismissFlags);
			builder.setDeleteIntent(dismissPendingIntent);
		} else {
			builder.setOngoing(true);
		}
		if (!infos.ticker.isEmpty()){
			builder.setTicker(infos.ticker);
		}
		
		builder.setPriority(Notification.PRIORITY_MAX);
		// Log.d(TAG, "Priority set to MAX");

		//If 5.0 >= set the controls to be visible on lockscreen
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			builder.setVisibility(Notification.VISIBILITY_PUBLIC);
			// Log.d(TAG, "Visibility set to PUBLIC");
		}

		//Set SmallIcon
		boolean usePlayingIcon = infos.notificationIcon.isEmpty();
		if(!usePlayingIcon){
			int resId = this.getResourceId(infos.notificationIcon, 0);
			usePlayingIcon = resId == 0;
			if(!usePlayingIcon) {
				builder.setSmallIcon(resId);
				// Log.d(TAG, "SmallIcon set from resource: " + infos.notificationIcon);
			}
		}

		if(usePlayingIcon){
			if (infos.isPlaying){
				builder.setSmallIcon(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play));
				// Log.d(TAG, "SmallIcon set to play icon");
			} else {
				builder.setSmallIcon(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause));
				// Log.d(TAG, "SmallIcon set to pause icon");
			}
		}

		//Set LargeIcon
		if (!infos.cover.isEmpty() && this.bitmapCover != null){
			builder.setLargeIcon(this.bitmapCover);
			// Log.d(TAG, "LargeIcon set");
		}

		//Open app if tapped
		Intent resultIntent = new Intent(context, cordovaActivity.getClass());
		resultIntent.setAction(Intent.ACTION_MAIN);
		resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire
		int resultFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, PENDING_INTENT_CONTENT, resultIntent, resultFlags);
		builder.setContentIntent(resultPendingIntent);
		// Log.d(TAG, "ContentIntent set");

		//Controls
		int nbControls=0;

		if (infos.hasPrev){
			/* Previous  */
			nbControls++;
			Intent previousIntent = new Intent("music-controls-previous");
			// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire pour les intents implicites
			int previousFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_PREVIOUS, previousIntent, previousFlags);
			builder.addAction(this.getResourceId(infos.prevIcon, android.R.drawable.ic_media_previous), "", previousPendingIntent);
			// Log.d(TAG, "Previous action added");
		}
		if (infos.isPlaying){
			/* Pause  */
			nbControls++;
			Intent pauseIntent = new Intent("music-controls-pause");
			// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire pour les intents implicites
			int pauseFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_PAUSE, pauseIntent, pauseFlags);
			builder.addAction(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause), "", pausePendingIntent);
			// Log.d(TAG, "Pause action added");
		} else {
			/* Play  */
			nbControls++;
			Intent playIntent = new Intent("music-controls-play");
			// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire pour les intents implicites
			int playFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_PLAY, playIntent, playFlags);
			builder.addAction(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play), "", playPendingIntent);
			// Log.d(TAG, "Play action added");
		}

		if (infos.hasNext){
			/* Next */
			nbControls++;
			Intent nextIntent = new Intent("music-controls-next");
			// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire pour les intents implicites
			int nextFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_NEXT, nextIntent, nextFlags);
			builder.addAction(this.getResourceId(infos.nextIcon, android.R.drawable.ic_media_next), "", nextPendingIntent);
			// Log.d(TAG, "Next action added");
		}
		if (infos.hasClose){
			/* Close */
			nbControls++;
			Intent destroyIntent = new Intent("music-controls-destroy");
			// CORRECTION ANDROID 16: FLAG_IMMUTABLE obligatoire pour les intents implicites
			int destroyFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
			PendingIntent destroyPendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_CLOSE, destroyIntent, destroyFlags);
			builder.addAction(this.getResourceId(infos.closeIcon, android.R.drawable.ic_menu_close_clear_cancel), "", destroyPendingIntent);
			// Log.d(TAG, "Close action added");
		}

		// Log.d(TAG, "Total actions: " + nbControls);

		//If 5.0 >= use MediaStyle
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			// Log.d(TAG, "Setting MediaStyle with " + nbControls + " actions");
			int[] args = new int[nbControls];
			for (int i = 0; i < nbControls; ++i) {
				args[i] = i;
			}
			Notification.MediaStyle mediaStyle = new Notification.MediaStyle()
				.setShowActionsInCompactView(args)
				.setMediaSession(this.token);
			
			// Log.d(TAG, "MediaStyle created, token=" + (this.token != null ? "NOT NULL" : "NULL"));
			builder.setStyle(mediaStyle);
		}
		this.notificationBuilder = builder;
		// Log.d(TAG, "Builder creation complete");
	}

	private int getResourceId(String name, int fallback){
		try{
			if(name.isEmpty()){
				return fallback;
			}

			int resId = this.cordovaActivity.getResources().getIdentifier(name, "drawable", this.cordovaActivity.getPackageName());
			return resId == 0 ? fallback : resId;
		}
		catch(Exception ex){
			return fallback;
		}
	}

	public void destroy(){
		// Log.d(TAG, "destroy called");
		this.notificationManager.cancel(this.notificationID);
		this.onNotificationDestroyed();
	}

	protected void onNotificationUpdated(Notification notification) {}
	protected void onNotificationDestroyed() {}
}
