package lu.knaff.alain.share_to_folder

/* This file is part of share-to-folder, an Android app to allow saving shared items to a folder
   Copyright (C) 2025,2026 Alain Knaff

   This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import java.net.URLDecoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.os.Bundle
import android.view.View
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.PopupMenu
import android.app.AlertDialog
import android.annotation.SuppressLint

import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope

import lu.knaff.alain.share_to_folder.db.TheDatabase
import lu.knaff.alain.share_to_folder.db.ShareTarget
import lu.knaff.alain.share_to_folder.db.Dao

class MainActivity : ComponentActivity() {
    private val TAG="MainActivity"

    fun getDao() : Dao {
        return TheDatabase.getDao(applicationContext)
    }

    lateinit var adapter: StfAdapter;

    val callback = object : ItemTouchHelper.SimpleCallback(
                                0, // No drag-and-drop
                                ItemTouchHelper.RIGHT
                            ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                adapter.removeAt(position)
            }
        }

        override fun getSwipeDirs(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return 0

            return if (adapter.shareTargets[position].isLocked()) {
                0
            } else {
                super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
    }

    lateinit var recyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        recyclerView=findViewById<RecyclerView>(R.id.share_targets)
        adapter=StfAdapter()
        recyclerView.adapter=adapter
        recyclerView.layoutManager=LinearLayoutManager(this)
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    override fun onResume()
    {
        super.onResume()
        (findViewById<RecyclerView>(R.id.share_targets).adapter
             as StfAdapter).updateData()
    }

    inner class StfAdapter():RecyclerView.Adapter<StfAdapter.ViewHolder>()
    {
        private val TAG="StfAdapter"
        var shareTargets = getDao().getAll()
        inner class ViewHolder(view:View):RecyclerView.ViewHolder(view),
                                          View.OnClickListener
        {
            private val TAG="StfAdapter.ViewHolder"
            // val binding = DataBindingUtil.setContentView(view)
            val text:TextView = view.findViewById(R.id.text)
            val icon:ImageView = view.findViewById(R.id.icon)
            val rem_preauth:Button = view.findViewById(R.id.rem_preauth)
            val subdir:Button = view.findViewById(R.id.subdir)
            val view = view
            lateinit var shareTarget:ShareTarget

            init {
                icon.setOnClickListener { v -> iconMenu(v) }
            }

            override fun onClick(view: View) {
            }

            fun iconMenu(view: View) {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION)
                    return
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.icon_menu, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    val item = shareTargets[position]
                    when (menuItem.itemId) {
                        R.id.action_set_custom_icon -> {
                            onSetCustomIcon(item, position)
                        }
                        R.id.action_clear_custom_icon -> {
                            onClearCustomIcon(item, position);
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {
            val view=LayoutInflater.from(parent.context).inflate(R.layout.share_target,parent,false)
            return ViewHolder(view)
        }

        private fun restyleHolder(holder: ViewHolder, st: ShareTarget) {
            holder.rem_preauth.visibility = if(st.always) View.VISIBLE else View.GONE
            holder.subdir.setText(if(st.subdirMode)
                                      R.string.subdir_on
                                  else
                                      R.string.subdir_off)
            holder.view.setBackgroundColor(ContextCompat
                                               .getColor(this@MainActivity,
                                                         if(st.always)
                                                             R.color.greenbg
                                                         else
                                                             R.color.redbg))

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int)
        {
            val shareTarget=shareTargets[position]

            val treeUri = shareTarget.uri.toUri()
            val authority = treeUri.authority!!
            val docIdEncoded = treeUri.lastPathSegment
            val docId = Uri.decode(docIdEncoded)

            holder.text.text=URLDecoder.decode(docId, "UTF-8")

            holder
                .icon
                .setImageBitmap(IconUtil.getBitmapForShareTarget(this@MainActivity,
                                                                 shareTarget))
            holder.icon.contentDescription = authority
            holder.shareTarget=shareTarget
            restyleHolder(holder, shareTarget)
            holder.rem_preauth.setOnClickListener()
            {
                getDao().setAlways(shareTarget,false)
                restyleHolder(holder, shareTarget)
            }
            holder.subdir.setOnClickListener()
            {
                getDao().toggleSubdir(shareTarget)
                restyleHolder(holder, shareTarget)
            }
        }

        override fun getItemCount(): Int
        {
            return shareTargets.size
        }

        fun updateData()
        {
            shareTargets=getDao().getAll()
            @SuppressLint("NotifyDataSetChanged")
            // not a huge list, and sometimes we cannot indeed
            // describe which position has changed exactly, such as
            // when *adding* a new item
            notifyDataSetChanged()
        }

        fun removeAt(position: Int) {
            var shareTarget=shareTargets[position]
            getDao().delete(shareTarget)
            ShortcutManagerCompat.removeDynamicShortcuts(
                applicationContext,
                listOf(shareTarget.uri)
            )
            shareTargets.removeAt(position)
            notifyItemRemoved(position)
        }

        private var setIconShareTarget : ShareTarget? = null;
        private var setIconPosition : Int? = null;

        fun finalizeIconChange(item: ShareTarget, position: Int) {
            notifyItemChanged(position)
            ShareReceiver.addOrRefreshShortcut(applicationContext, item, false);
        }

        private val uploadIcon =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                uri ->
                val st = setIconShareTarget
                if (uri == null || st == null) {
                    return@registerForActivityResult
                }

                lifecycleScope.launch {
                    recyclerView.suppressLayout(true)
                    try {
                        withContext(Dispatchers.IO) {
                            val destination = IconUtil
                                .getIconFile(this@MainActivity, st.uid,
                                             true);
                            contentResolver.openInputStream(uri)?.use { input ->
                                destination.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            } ?: error("Could not open selected icon")
                        }
                        finalizeIconChange(st, setIconPosition!!)
                    } catch (e: Exception) {
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(this@MainActivity)
                        builder
                            .setMessage(e.message)
                            .show()
                    } finally {
                        recyclerView.suppressLayout(false)
                        setIconShareTarget = null;
                    }
                }
            }

        fun onSetCustomIcon(item: ShareTarget, position: Int): Boolean {
            if(setIconShareTarget != null) {
                val builder: AlertDialog.Builder =
                    AlertDialog.Builder(this@MainActivity)
                builder
                    .setMessage("Icon upload already in progress")
                    .show()
            }
            setIconShareTarget=item
            setIconPosition = position
            uploadIcon.launch(arrayOf("image/*"))
            return true
        }

        fun onClearCustomIcon(item: ShareTarget, position: Int): Boolean {
            val file = IconUtil.getIconFile(this@MainActivity, item.uid,
                                            false)
            file.delete()
            finalizeIconChange(item, position)
            return true
        }
    }
}
