package lu.knaff.alain.share_to_folder

/* This file is part of share-to-folder, an Android app to allow saving shared items to a folder
   Copyright (C) 2025,2026 Alain Knaff

   This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import java.io.FileNotFoundException
import java.net.URLDecoder
import android.os.Build

import android.util.Log
import android.os.Bundle
import android.content.Intent
import android.app.AlertDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat

import android.webkit.MimeTypeMap

import androidx.core.net.toUri

import lu.knaff.alain.share_to_folder.db.TheDatabase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

import android.provider.DocumentsContract

/**
 * This activity receives share requests from other applications
 */
class ShareReceiver : AppCompatActivity(), CoroutineScope by MainScope()  {
    private val TAG="ShareReceiver"

    fun getLastPathPart(uri:Uri?):String? {
	val filename=uri?.lastPathSegment
	if(filename==null)
	    return null
	val pos=filename.lastIndexOf('/')
	if(pos >= 0)
	    return filename.substring(pos+1)
	else
	    return filename
    }

    fun addOrRefreshShortcut(treeUri:Uri, isNew:Boolean) {
        if(isNew)
            contentResolver
                .takePersistableUriPermission(treeUri,
                                              Intent.FLAG_GRANT_READ_URI_PERMISSION  or
                                              Intent.FLAG_GRANT_WRITE_URI_PERMISSION )
        val uriString:String=treeUri.toString()
        var shortLabel=getLastPathPart(treeUri)
        if(shortLabel==null)
            shortLabel="folder"

        val authority = treeUri.authority!!
        var bits = IconUtil.getBitmapForAuthority(this, authority)

        val icon = IconCompat.createWithBitmap(bits)

        val shortcutInfo =
            ShortcutInfoCompat.Builder(applicationContext, uriString)
            .setShortLabel(shortLabel)
            .setIcon(icon)
            .setLongLived(true)
            .setCategories(setOf("lu.knaff.alain.share_to_folder.category.TEXT_SHARE_TARGET"))
            .setIntent(Intent(Intent.ACTION_DEFAULT))
            .build()
        if(isNew)
            ShortcutManagerCompat.addDynamicShortcuts(applicationContext,
                                                      listOf(shortcutInfo))
        ShortcutManagerCompat.pushDynamicShortcut(applicationContext,
                                                  shortcutInfo)
    }

    fun processPickDirectory(treeUri:Uri?, addShortcut: Boolean) {
        if(treeUri == null) {
            // User went back
            finish()
            return
        }
        if(addShortcut)
            addOrRefreshShortcut(treeUri, true)
        saveFileTo(treeUri)
        TheDatabase
            .getDao(applicationContext)
            .getOrCreate(treeUri.toString())
    }

    fun error(msg: String) {
        runOnUiThread {
            AlertDialog
                .Builder(this@ShareReceiver)
                .setMessage(getString(R.string.error, msg))
                .setPositiveButton(R.string.ok) {
                    d, w -> finish()
                }
                .show()
        }
    }

    fun saveFileTo(treeUri:Uri) {
        launch {
            try {
                if(intent.action == Intent.ACTION_SEND) {
                    saveOneFileTo(treeUri)
                } else if(intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    saveMultipleFilesTo(treeUri)
                } else {
                    throw IllegalArgumentException("Intent "+intent.action+
                                                       "not supported")
                }
                runOnUiThread { finish() }
            } catch(e: Exception) {
                Log.e(TAG, "Exception while saving shared data", e)
                var t : Throwable = e
                while(t.cause != null)
                    t = t.cause!!
                error(t.toString())
            }
        }
    }

    fun saveOneFileTo(treeUri:Uri) {
        var srcUri:Uri? = intent.data
        if(srcUri==null) {
            val o:Any?=
                @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if(o is Uri)
                srcUri=o
        }
        saveFileTo(treeUri, srcUri)
    }

    fun saveMultipleFilesTo(treeUri:Uri) {
        val list: Collection<Any> = @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!
        for(srcUri in list)
            if(srcUri is Uri)
                saveFileTo(treeUri, srcUri)
            else
                Log.i(TAG, "Ignoring "+srcUri+" which is not a Uri")
    }

    fun saveFileTo(treeUri:Uri, srcUri: Uri?) {
        var filename=getLastPathPart(srcUri)
        if(filename == null) {
            filename="file.txt"
        }

        var mimeType:String? = intent.type
        if(mimeType==null || mimeType=="null")
            mimeType="text/plain"

        if(filename.indexOf('.')==-1) {
            // filename contains no dot, append extension according to mime type
            var ext: String? = MimeTypeMap
                .getSingleton()
                .getExtensionFromMimeType(mimeType)
            if(ext == null) {
                if(mimeType == "image/*")
                    ext = "jpg"
                else if(mimeType == "video/*")
                    ext = "mp4"
                else {
                    Log.i(TAG, "Unknown mime type: "+mimeType)
                    ext = "bin"
                }
            }
            filename = filename + "."+ext
        }

        val directory = DocumentFile
            .fromTreeUri(this@ShareReceiver, treeUri)
        if(directory==null)
            throw FileNotFoundException("Could not open directory "+treeUri)

        val destFile = directory.createFile(mimeType,filename)
        if(destFile==null)
            throw FileNotFoundException("Could not create file "+filename)

        val outStream = contentResolver.openOutputStream(destFile.uri)
        if(outStream == null)
            throw FileNotFoundException("Could not create output stream for "+
                                            filename)

        // copy input to output
        if(srcUri != null) {
            val inStream = contentResolver.openInputStream(srcUri)
            if(inStream==null)
                throw FileNotFoundException("Could not read "+srcUri)
            inStream.copyTo(outStream)
            inStream.close()
        } else {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            if(subject != null)
                outStream.write((subject+"\n").toByteArray())

            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if(text != null)
                outStream.write((text+"\n").toByteArray())
        }
        outStream.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = if(Build.VERSION.SDK_INT >= 29)
            intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
        else
            null
        if(key == null) {
            launchPicker()
            return
        }

        val dao = TheDatabase.getDao(applicationContext)
        val st = dao.getOrCreate(key)
        if(st.subdirMode) {
            launchPicker(key)
            return
        }
        if(!st.always) {
            // display confirmation dialog here
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage(getString(R.string.share_confirm,
                                      URLDecoder.decode(key, "UTF-8")))
                .setNegativeButton(R.string.no) { d, w  -> finish() }
                .setNeutralButton(R.string.yes) { d, w -> doSaveFileTo(key) }
                .setPositiveButton(R.string.always) {
                    d, w ->
                    dao.setAlways(key, true)
                    doSaveFileTo(key)
                }
                .show()
            return
        }

        doSaveFileTo(key)
    }

    fun doSaveFileTo(key: String) {
        val uri=key.toUri()
        addOrRefreshShortcut(uri,false)
        saveFileTo(uri)
    }

    val launcher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        result-> processPickDirectory(result, true)
    }

    fun launchPicker() {
        launcher.launch(null)
    }

    val launcherMakeNoShortcut = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        result-> processPickDirectory(result, false)
    }

    fun uriStrToDocumentId(uriStr: String): Uri {
        val treeUri = uriStr.toUri()
        val authority = treeUri.authority
        val docIdEncoded = treeUri.lastPathSegment
        val docId = Uri.decode(docIdEncoded)
        return DocumentsContract.buildDocumentUri(authority, docId)
    }

    fun launchPicker(uriStr: String) {
        launcherMakeNoShortcut.launch(uriStrToDocumentId(uriStr))
    }
}
