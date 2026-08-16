package lu.knaff.alain.share_to_folder.db

/* This file is part of share-to-folder, an Android app to allow saving shared items to a folder
 Copyright (C) 2026 Alain Knaff

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "share_target")
data class ShareTarget(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "always") var always: Boolean,
    @ColumnInfo(name = "subdir_mode", defaultValue = "false") var subdirMode: Boolean
)
