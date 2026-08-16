package lu.knaff.alain.share_to_folder.db

/* This file is part of share-to-folder, an Android app to allow saving shared items to a folder
 Copyright (C) 2026 Alain Knaff

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Transaction

@Dao
interface Dao {
    @Query("SELECT * FROM share_target")
    fun getAll(): MutableList<ShareTarget>

    @Query("SELECT * FROM share_target WHERE uri = :uri")
    fun loadByUri(uri: String): ShareTarget?

    @Query("SELECT * FROM share_target WHERE uid = :uid")
    fun loadByUid(uid: Int): ShareTarget?

    @Insert
    fun insertAll(vararg targets: ShareTarget)

    @Update
    fun update(target: ShareTarget)

    @Delete
    fun delete(user: ShareTarget)

    @Transaction
    fun getOrCreate(uri: String): ShareTarget {
	var st = loadByUri(uri)
	if(st == null) {
	    st = ShareTarget(0, uri, false, false)
	    insertAll(st)
	}
	return st
    }

    @Transaction
    fun setAlways(uri: String, always: Boolean) {
	val st = loadByUri(uri)
	if(st != null) {
	    st.always = always
	    update(st)
	}
    }

    @Transaction
    fun setAlways(st: ShareTarget, always: Boolean) {
	st.always = always
	update(st)
    }

    @Transaction
    fun setSubdir(st: ShareTarget, subdir: Boolean) {
	st.subdirMode = subdir
	update(st)
    }
}
