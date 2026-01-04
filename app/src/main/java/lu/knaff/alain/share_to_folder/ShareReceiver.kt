package lu.knaff.alain.share_to_folder

import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import android.os.Build

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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint

import androidx.core.net.toUri

import lu.knaff.alain.share_to_folder.db.TheDatabase

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
	val uriString:String=treeUri.toString()
	var shortLabel=getLastPathPart(treeUri)
	if(shortLabel==null)
	    shortLabel="folder"

	val textPaint = TextPaint()
	textPaint.setTextSize(66f)
	textPaint.setColor(Color.BLACK);
	val bits = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888);
	val canvas = Canvas(bits)

	try {
	    val authority = treeUri.authority!!;
	    val docs=authority.lastIndexOf(".documents")
	    val idx= if(docs > 0)
		authority.lastIndexOf('.', docs-1)
	    else
		authority.lastIndexOf('.')
	    val letter = if(idx == -1)
		authority.substring(0,1)
	    else
		authority.substring(idx+1,idx+2)
	    canvas.drawText(letter.uppercase(), 21f, 87f, textPaint);
	} catch(e : Exception) {
	    // if an exception occurs, just don't draw any text...
	}

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

    fun processPickDirectory(treeUri:Uri?) {
	if(treeUri == null) {
	    // User went back
	    finish()
	    return
	}
	addOrRefreshShortcut(treeUri,true)
	saveFileTo(treeUri)
	TheDatabase
	    .getDao(applicationContext)
	    .getOrCreate(treeUri.toString())
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
		.show();
	    return;
	}

	doSaveFileTo(key)
    }

    fun doSaveFileTo(key: String) {
	val uri=key.toUri()
	addOrRefreshShortcut(uri,false)
	saveFileTo(uri)
    }

    fun launchPicker() {
	val launcher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
	    result-> processPickDirectory(result)
	}
	launcher.launch(null)
    }
}
