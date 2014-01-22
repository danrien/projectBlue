package com.lasthopesoftware.bluewater.data.session;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.JrAccessDao;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;

public class JrSession {
	public static final String PREFS_FILE = "com.lasthopesoftware.jrmediastreamer.PREFS";
	private static final String PLAYLIST_KEY = "Playlist";
	private static final String NOW_PLAYING_KEY = "now_playing";
	private static final String NP_POSITION = "np_position";
	private static final String ACCESS_CODE_KEY = "access_code";
	private static final String USER_AUTH_CODE_KEY = "user_auth_code";
	private static final String IS_LOCAL_ONLY = "is_local_only";
	private static final String LIBRARY_KEY = "library_KEY";

	public static boolean IsLocalOnly = false;

	public static String UserAuthCode = "";
	public static String AccessCode = "";

	public static JrAccessDao accessDao;

	private static int[] SelectedLibraryKeys = new int[0];

	public static IJrItem<?> SelectedItem;
	public static JrFile PlayingFile;
	//    public static ArrayList<JrFile> Playlist;
	public static String Playlist;

	public static JrFileSystem JrFs;

	private static boolean mActive = false;

	public static void SaveSession(Context context) {
		SaveSession(context.getSharedPreferences(PREFS_FILE, 0).edit());
	}

	public static void SaveSession(SharedPreferences.Editor prefsEditor) {
		prefsEditor.putString(ACCESS_CODE_KEY, AccessCode);
		prefsEditor.putString(USER_AUTH_CODE_KEY, UserAuthCode);
		prefsEditor.putBoolean(IS_LOCAL_ONLY, IsLocalOnly);
		prefsEditor.putStringSet(LIBRARY_KEY, getLibraryKeysSet());
		
		if (Playlist != null) {
			prefsEditor.putString(PLAYLIST_KEY, Playlist);
		}

		if (PlayingFile != null) {
			prefsEditor.putInt(NOW_PLAYING_KEY, PlayingFile.getKey());
			prefsEditor.putInt(NP_POSITION, PlayingFile.getCurrentPosition());
		}

		prefsEditor.apply();
		
		Logger log = LoggerFactory.getLogger(JrSession.class);
		log.info("Session saved.");
	}

	public static boolean CreateSession(Context context) {
		return CreateSession(context.getSharedPreferences(PREFS_FILE, 0));
	}

	public static boolean CreateSession(SharedPreferences prefs) {
		Logger log = LoggerFactory.getLogger(JrSession.class);
		log.info("Session started.");
		
		AccessCode = prefs.getString(ACCESS_CODE_KEY, "");
		UserAuthCode = prefs.getString(USER_AUTH_CODE_KEY, "");
		IsLocalOnly = prefs.getBoolean(IS_LOCAL_ONLY, false);
		setLibraryKeys(prefs.getStringSet(LIBRARY_KEY, new HashSet<String>()));
		mActive = false;
		
		if (JrSession.AccessCode == null || JrSession.AccessCode.isEmpty() || !tryConnection()) return false;
		
		if (JrSession.JrFs == null) JrSession.JrFs = new JrFileSystem(getLibraryKeys());
		mActive = true;

		try {
			Playlist = prefs.getString(PLAYLIST_KEY, "");
		} catch (ClassCastException ce) {
			Playlist = null;
			return mActive;
		}

		int savedFileKey = prefs.getInt(NOW_PLAYING_KEY, -1);
		int savedFilePos = prefs.getInt(NP_POSITION, -1);


		if (savedFileKey < 0) return mActive;
		String savedFileKeyString = String.valueOf(savedFileKey);
		for (String fileKey : Playlist.split(";")) {
			if (!savedFileKeyString.equals(fileKey)) continue;
			PlayingFile = new JrFile(savedFileKey);
			if (savedFilePos > -1) PlayingFile.seekTo(savedFilePos);
		}

		return mActive;
	}
	
	public static void setLibraryKeys(int[] keys) {
		SelectedLibraryKeys = keys;
	}
	
	public static void setLibraryKeys(Set<String> keys) {
		int i = 0;
		SelectedLibraryKeys = new int[keys.size()];
		for (String key : keys)
			SelectedLibraryKeys[i++] = Integer.parseInt(key);
	}
	
	public static boolean isActive() {
		return mActive;
	}
	
	public static int[] getLibraryKeys() {
		return SelectedLibraryKeys;
	}
	
	public static HashSet<String> getLibraryKeysSet() {
		HashSet<String> libraryKeys = new HashSet<String>(SelectedLibraryKeys.length);
		for (int key : SelectedLibraryKeys)
			libraryKeys.add(String.valueOf(key));
		
		return libraryKeys;
	}

	private static boolean tryConnection() {
		boolean connectResult = false;
		try {
			JrSession.accessDao = new GetMcAccess().execute(JrSession.AccessCode).get();
			connectResult = !JrSession.accessDao.getActiveUrl().isEmpty();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return connectResult;
	}

	private static class GetMcAccess extends AsyncTask<String, Void, JrAccessDao> {

		@Override
		protected JrAccessDao doInBackground(String... params) {

			JrAccessDao accessDao = null;
			try {
				accessDao = new JrAccessDao();
				
				if (UrlValidator.getInstance().isValid(params[0])) {
					Uri jrUrl = Uri.parse(params[0]);
					accessDao.setRemoteIp(jrUrl.getHost());
					accessDao.setPort(jrUrl.getPort());
					accessDao.setStatus(true);
					IsLocalOnly = false;
				} else {
					URLConnection conn = (new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + params[0])).openConnection();
					XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
					
					
					accessDao.setStatus(xml.getAttribute("Status").equalsIgnoreCase("OK"));
					accessDao.setPort(Integer.parseInt(xml.getUnique("port").getValue()));
					accessDao.setRemoteIp(xml.getUnique("ip").getValue());
					for (String localIp : xml.getUnique("localiplist").getValue().split(","))
						accessDao.getLocalIps().add(localIp);
					for (String macAddress : xml.getUnique("macaddresslist").getValue().split(","))
						accessDao.getMacAddresses().add(macAddress);
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return accessDao;
		}
	}
}
