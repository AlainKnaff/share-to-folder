package lu.knaff.alain.share_to_folder

import java.io.InputStream
import java.io.OutputStream

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import android.annotation.TargetApi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * This activity receives share requests from other applications
 */
class ShareReceiver : AppCompatActivity(), CoroutineScope by MainScope()  {
    private val TAG="ShareReceiver"

    fun getLastPathPart(uri:Uri):String? {
	val filename=uri.getLastPathSegment()
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
					      Intent.FLAG_GRANT_WRITE_URI_PERMISSION );
	val uriString:String=treeUri!!.toString()
	var shortLabel=getLastPathPart(treeUri)
	if(shortLabel==null)
	    shortLabel="folder"
	val shortcutInfo =
	    ShortcutInfoCompat.Builder(applicationContext, uriString)
	    .setShortLabel(shortLabel)
	    .setIcon(IconCompat.createWithResource(applicationContext,
						   R.drawable.ic_launcher_foreground))
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

    fun processPickDirectory(treeUri:Uri?) {
	if(treeUri == null) {
	    // User went back
	    finish()
	    return
	}
	addOrRefreshShortcut(treeUri,true)
	saveFileTo(treeUri)
    }

    fun saveFileTo(treeUri:Uri) {
	var srcUri:Uri? = intent.getData()
	if(srcUri==null) {
	    val o:Any?=
		@Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
	    if(o is Uri)
		srcUri=o
	}
	var filename=getLastPathPart(srcUri!!)
	if(filename == null) {
	    filename="file"
	}

	launch {
	    var mimeType:String? = intent.type
	    if(mimeType=="null")
		mimeType="text/plain"
	    val inStream:InputStream =  contentResolver.openInputStream(srcUri)!!

	    val destFile:DocumentFile = DocumentFile
		.fromTreeUri(this@ShareReceiver, treeUri)
		?.createFile(mimeType!!,filename)!!

	    val outStream:OutputStream =
		contentResolver.openOutputStream(destFile.uri)!!

	    // copy input to output
	    inStream.copyTo(outStream);
	    inStream.close()
	    outStream.close()
	    runOnUiThread { finish() }
	}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
	super.onCreate(savedInstanceState)
	var key:String?=null
	@TargetApi(29) {
	    key = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
	}
	if(key != null) {
	    val uri=Uri.parse(key)
	    addOrRefreshShortcut(uri,false)
	    saveFileTo(uri)
	} else
	    launchPicker()
    }

    fun launchPicker() {
	val launcher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
	    result-> processPickDirectory(result)
	}
	launcher.launch(null)
    }
}
