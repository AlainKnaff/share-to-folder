package lu.knaff.alain.share_to_folder

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

    private fun getDao() : Dao {
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
	    val button:Button = view.findViewById(R.id.button)
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

	private fun restyleHolder(holder: ViewHolder, always: Boolean) {
	    holder.button.setVisibility(if(always) View.VISIBLE else View.GONE)
	    holder.view.setBackgroundColor(ContextCompat
					       .getColor(this@MainActivity,
							 if(always)
							     R.color.lightgreen
							 else
							     R.color.lightred))

	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int)
	{
	    val shareTarget=shareTargets[position]
	    holder.text.text=URLDecoder.decode(shareTarget.uri, "UTF-8")
	    holder.shareTarget=shareTarget
	    restyleHolder(holder, shareTarget.always)
	    holder.button.setOnClickListener()
	    {
		getDao().setAlways(shareTarget.uri,false)
		restyleHolder(holder, false)
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
