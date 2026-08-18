package lu.knaff.alain.share_to_folder

import androidx.core.graphics.createBitmap
import android.text.TextPaint
import android.util.Log
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo 
import android.content.Context

class IconUtil {
    companion object {
	private val TAG="IconUtil"

	fun getPackageForAuthority(ctx: Context,
				   authority: String) : ApplicationInfo {
            try {
		var providerPkg = authority.removeSuffix(".documents")
		return ctx.packageManager.getApplicationInfo(providerPkg, 0)
            } catch(e: PackageManager.NameNotFoundException) {
		val cps = ctx.packageManager
	            .queryContentProviders(
			null,
			0,
			0
	            )
		val packageName = cps.firstOrNull {
	            it.authority == authority
		}?.packageName
		return ctx.packageManager.getApplicationInfo(packageName!!, 0)
            }
	}

	fun getBitmapForAuthority(ctx: Context, authority: String) : Bitmap {
            try {
		val appInfo = getPackageForAuthority(ctx, authority)
		val icon = ctx.packageManager.getApplicationIcon(appInfo)
		var bits :Bitmap? = null
		if(icon is BitmapDrawable) {
                    return icon.bitmap
		} else {
                    val bits = createBitmap(icon.intrinsicWidth.coerceAtLeast(1),
                                            icon.intrinsicHeight.coerceAtLeast(1),
                                            Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bits)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    return bits
		}
            } catch (e: Exception) {
		Log.i(TAG, "Exception while getting icon ",e)
            }

            // fallback icon
            val bits = createBitmap(108, 108, Bitmap.Config.ARGB_8888)
            val paint = Paint()
            paint.setColor(Color.LTGRAY)

            val textPaint = TextPaint()
            textPaint.setTextSize(66f)
            textPaint.setTextAlign(Paint.Align.CENTER)
            textPaint.setColor(Color.BLACK)
            val canvas = Canvas(bits)
            canvas.drawCircle(54f,54f,50f, paint)

            try {
		val docs=authority.lastIndexOf(".documents")
		val idx= if(docs > 0)
                    authority.lastIndexOf('.', docs-1)
		else
                    authority.lastIndexOf('.')
		val letter = if(idx == -1)
                    authority.substring(0,1)
		else
                    authority.substring(idx+1,idx+2)
		canvas.drawText(letter.uppercase(), 54f, 78f, textPaint)
            } catch(e : Exception) {
		// if an exception occurs, just don't draw any text...
            }
            return bits
	}
    }
}
