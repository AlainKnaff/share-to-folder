package lu.knaff.alain.share_to_folder

/* This file is part of share-to-folder, an Android app to allow saving shared items to a folder
 Copyright (C) 2025,2026 Alain Knaff

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

import java.net.URLDecoder

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.annotation.SuppressLint

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat

import lu.knaff.alain.share_to_folder.db.TheDatabase
import lu.knaff.alain.share_to_folder.db.ShareTarget
import lu.knaff.alain.share_to_folder.db.Dao



class MainActivity : Activity() {
    private val TAG="MainActivity"

    fun getDao() : Dao {
	return TheDatabase.getDao(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
	super.onCreate(savedInstanceState)
	setContentView(R.layout.main)

	val recyclerView=findViewById<RecyclerView>(R.id.share_targets)
	recyclerView.adapter=StfAdapter(this)
	recyclerView.layoutManager=LinearLayoutManager(this)
    }

    /*
    fun editShareTarget(view:View, shareTarget:ShareTarget)
    {
	val intent:Intent = Intent(this, AuthenticationActivity::class.java)
	intent.putExtra(DBHandler.ID_COL, shareTarget.id)
	startActivity(intent)
    }
    */

    override fun onResume()
    {
	super.onResume()
	(findViewById<RecyclerView>(R.id.share_targets).adapter
	     as StfAdapter).updateData()
    }

    inner class StfAdapter(private val activity:Activity):RecyclerView.Adapter<StfAdapter.ViewHolder>()
    {
	private val TAG="StfAdapter"
	private var shareTargets = getDao().getAll()
	inner class ViewHolder(view:View):RecyclerView.ViewHolder(view),
					  View.OnClickListener
	{
	    private val TAG="StfAdapter.ViewHolder"
	    // val binding = DataBindingUtil.setContentView(view)
	    val text:TextView = view.findViewById(R.id.text)
	    val rem_preauth:Button = view.findViewById(R.id.rem_preauth)
	    val act_subdir:Button = view.findViewById(R.id.act_subdir)
	    val rem_subdir:Button = view.findViewById(R.id.rem_subdir)
	    val view = view;
	    public lateinit var shareTarget:ShareTarget

	    init {
		view.setOnClickListener(this)
		text.setOnClickListener(this)
	    }

	    override fun onClick(view: View)
	    {
		// editShareTarget(view,shareTarget)
	    }
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
	{
	    val view=LayoutInflater.from(parent.context).inflate(R.layout.share_target,parent,false)
	    return ViewHolder(view)
	}

	private fun restyleHolder(holder: ViewHolder, st: ShareTarget) {
	    holder.rem_preauth.setVisibility(if(st.always) View.VISIBLE else View.GONE)
	    holder.act_subdir.setVisibility(if(!st.subdirMode) View.VISIBLE else View.GONE)
	    holder.rem_subdir.setVisibility(if(st.subdirMode) View.VISIBLE else View.GONE)
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
	    holder.text.text=URLDecoder.decode(shareTarget.uri, "UTF-8")
	    holder.shareTarget=shareTarget
	    restyleHolder(holder, shareTarget)
	    holder.rem_preauth.setOnClickListener()
	    {
		getDao().setAlways(shareTarget,false)
		restyleHolder(holder, shareTarget)
	    }
	    holder.act_subdir.setOnClickListener()
	    {
		getDao().setSubdir(shareTarget,true)
		restyleHolder(holder, shareTarget)
	    }
	    holder.rem_subdir.setOnClickListener()
	    {
		getDao().setSubdir(shareTarget,false)
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
    }
}
