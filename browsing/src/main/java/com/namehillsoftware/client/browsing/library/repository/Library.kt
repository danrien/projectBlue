package com.namehillsoftware.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.IEntityCreator
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import java.util.*

@Keep
data class Library(
        private var _id: Int = -1,
        private var _libraryName: String? = null,
        private var _accessCode: String? = null,
        private var _userName: String? = null,
        private var _password: String? = null,
        private var _isLocalOnly: Boolean = false,
        private var _isRepeating: Boolean = false,
        private var _nowPlayingId: Int = -1,
        private var _nowPlayingProgress: Long = -1,
        private var _selectedViewType: ViewType? = null,
        private var _selectedView: Int = -1,
        private var _savedTracksString: String? = null,
        private var _customSyncedFilesPath: String? = null,
        private var _syncedFileLocation: SyncedFileLocation? = null,
        private var _isUsingExistingFiles: Boolean = false,
        private var _isSyncLocalConnectionsOnly: Boolean = false,
        private var _isWakeOnLanEnabled: Boolean = false) : IEntityCreator, IEntityUpdater {

	val id: Int get() = _id
	val libraryName: String? get() = _libraryName
	val accessCode: String? get() = _accessCode
	val userName: String? get() = _userName
	val password: String? get() = _password
	val isLocalOnly: Boolean get() = _isLocalOnly
	val isRepeating: Boolean get() = _isRepeating
	val nowPlayingId: Int get() = _nowPlayingId
	val nowPlayingProgress: Long get() = _nowPlayingProgress
	val selectedViewType: ViewType? get() = _selectedViewType
	val selectedView: Int get() = _selectedView
	val savedTracksString: String? get() = _savedTracksString
	val customSyncedFilesPath: String? get() = _customSyncedFilesPath
	val syncedFileLocation: SyncedFileLocation? get() = _syncedFileLocation
	val isUsingExistingFiles: Boolean get() = _isUsingExistingFiles
	val isSyncLocalConnectionsOnly: Boolean get() = _isSyncLocalConnectionsOnly
	val isWakeOnLanEnabled: Boolean get() = _isWakeOnLanEnabled

	/**
	 * @param nowPlayingId the nowPlayingId to set
	 */
	fun setNowPlayingId(nowPlayingId: Int): Library {
		_nowPlayingId = nowPlayingId
		return this
	}

	/**
	 * @param libraryName the mLibraryName to set
	 */
	fun setLibraryName(libraryName: String?): Library {
		_libraryName = libraryName
		return this
	}

	/**
	 * @param accessCode the mAccessCode to set
	 */
	fun setAccessCode(accessCode: String?): Library {
		_accessCode = accessCode
		return this
	}

	/**
	 * @param nowPlayingProgress the nowPlayingProgress to set
	 */
	fun setNowPlayingProgress(nowPlayingProgress: Long): Library {
		_nowPlayingProgress = nowPlayingProgress
		return this
	}

	fun setSavedTracksString(savedTracksString: String?): Library {
		_savedTracksString = savedTracksString
		return this
	}

	/**
	 * @param isLocalOnly the isLocalOnly to set
	 */
	fun setLocalOnly(isLocalOnly: Boolean): Library {
		_isLocalOnly = isLocalOnly
		return this
	}

	/**
	 * @param selectedView the selectedView to set
	 */
	fun setSelectedView(selectedView: Int): Library {
		_selectedView = selectedView
		return this
	}

	/**
	 * @param isRepeating the isRepeating to set
	 */
	fun setRepeating(isRepeating: Boolean): Library {
		_isRepeating = isRepeating
		return this
	}

	fun setCustomSyncedFilesPath(customSyncedFilesPath: String?): Library {
		this._customSyncedFilesPath = customSyncedFilesPath
		return this
	}

	fun setSyncedFileLocation(syncedFileLocation: SyncedFileLocation?): Library {
		_syncedFileLocation = syncedFileLocation
		return this
	}

	fun setIsUsingExistingFiles(isUsingExistingFiles: Boolean): Library {
		_isUsingExistingFiles = isUsingExistingFiles
		return this
	}

	fun setIsSyncLocalConnectionsOnly(isSyncLocalConnections: Boolean): Library {
		_isSyncLocalConnectionsOnly = isSyncLocalConnections
		return this
	}

	fun setIsWakeOnLanEnabled(isWakeOnLanEnabled: Boolean): Library {
		_isWakeOnLanEnabled = isWakeOnLanEnabled
		return this
	}

	fun setSelectedViewType(selectedViewType: ViewType?): Library {
		_selectedViewType = selectedViewType
		return this
	}

	fun setId(id: Int): Library {
		_id = id
		return this
	}

	fun setUserName(userName: String?): Library {
		_userName = userName
		return this
	}

	fun setPassword(password: String?): Library {
		_password = password
		return this
	}

	val libraryId: LibraryId
		get() = LibraryId(id)

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE `LIBRARIES` (" +
			"`accessCode` VARCHAR(30) , " +
			"`userName` VARCHAR , " +
			"`password` VARCHAR , " +
			"`customSyncedFilesPath` VARCHAR , " +
			"`id` INTEGER PRIMARY KEY AUTOINCREMENT , " +
			"`isLocalOnly` SMALLINT , " +
			"`isRepeating` SMALLINT , " +
			"`isSyncLocalConnectionsOnly` SMALLINT , " +
			"`isUsingExistingFiles` SMALLINT , " +
			"`" + isWakeOnLanEnabledColumn + "` SMALLINT , " +
			"`libraryName` VARCHAR(50) , " +
			"`nowPlayingId` INTEGER DEFAULT -1 NOT NULL , " +
			"`nowPlayingProgress` INTEGER DEFAULT -1 NOT NULL , " +
			"`savedTracksString` VARCHAR , " +
			"`selectedView` INTEGER DEFAULT -1 NOT NULL , " +
			"`selectedViewType` VARCHAR , " +
			"`syncedFileLocation` VARCHAR )")
	}

	override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 5) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `isSyncLocalConnectionsOnly` BOOLEAN DEFAULT 0;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `selectedViewType` VARCHAR;")
		}
		if (oldVersion < 7) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `userName` VARCHAR;")
			db.execSQL("ALTER TABLE `LIBRARIES` add column `password` VARCHAR;")
			db.rawQuery("SELECT ID, authKey FROM `LIBRARIES` WHERE `authKey` IS NOT NULL AND `authKey` <> ''", arrayOfNulls(0)).use { cursor ->
				if (cursor.moveToFirst() && cursor.count > 0) {
					do {
						val libraryId = cursor.getInt(0)
						val authKey = cursor.getString(1)
						if (authKey == null || authKey.isEmpty()) continue
						val decodedAuthKey = String(Base64.decode(authKey, Base64.DEFAULT))
						val userCredentials = decodedAuthKey.split(":").toTypedArray()
						if (userCredentials.size > 1) {
							db.execSQL(
								"UPDATE `" + tableName + "` " +
									" SET `" + userNameColumn + "` = ?, " +
									" `" + passwordColumn + "` = ? " +
									" WHERE `id` = ?", arrayOf<Any>(
								userCredentials[0],
								userCredentials[1],
								libraryId
							))
							continue
						}
						if (userCredentials.isNotEmpty()) {
							db.execSQL(
								"UPDATE `" + tableName + "` " +
									" SET `" + userNameColumn + "` = ? " +
									" WHERE `id` = ?", arrayOf<Any>(
								userCredentials[0],
								libraryId
							))
						}
					} while (cursor.moveToNext())
				}
			}
		}
		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE `LIBRARIES` add column `$isWakeOnLanEnabledColumn` SMALLINT;")
		}
	}

	@Keep
	enum class SyncedFileLocation {
		EXTERNAL, INTERNAL, CUSTOM;

		companion object {
			@JvmField
			val ExternalDiskAccessSyncLocations: Set<SyncedFileLocation> = Collections.unmodifiableSet(
				HashSet(
					listOf(EXTERNAL,
						CUSTOM)))
		}
	}

	@Keep
	enum class ViewType {
		StandardServerView, PlaylistView, DownloadView
	}

	companion object {
		const val tableName = "LIBRARIES"
		const val libraryNameColumn = "libraryName"
		const val accessCodeColumn = "accessCode"
		const val isLocalOnlyColumn = "isLocalOnly"
		const val isRepeatingColumn = "isRepeating"
		const val nowPlayingIdColumn = "nowPlayingId"
		const val nowPlayingProgressColumn = "nowPlayingProgress"
		const val selectedViewTypeColumn = "selectedViewType"
		const val selectedViewColumn = "selectedView"
		const val savedTracksStringColumn = "savedTracksString"
		const val customSyncedFilesPathColumn = "customSyncedFilesPath"
		const val syncedFileLocationColumn = "syncedFileLocation"
		const val isUsingExistingFilesColumn = "isUsingExistingFiles"
		const val isSyncLocalConnectionsOnlyColumn = "isSyncLocalConnectionsOnly"
		const val userNameColumn = "userName"
		const val passwordColumn = "password"
		const val isWakeOnLanEnabledColumn = "isWakeOnLanEnabled"
	}
}
