package cn.cb.testapp.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.cb.testapp.databinding.ItemMainBinding

class MyAdapter : Adapter<MyAdapter.MyViewHolder>() {

    val list = mutableListOf<MainItem>()
    private var mListener: ((item: MainItem, position: Int) -> Unit)? = null

    class MyViewHolder(val view: ItemMainBinding) : RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ItemMainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.view.item = list[position]
        holder.view.root.setOnClickListener {
            mListener?.apply { invoke(list[position], position) }
        }
    }

    fun setOnItemClickListener(listener: (MainItem, Int) -> Unit) {
        mListener = listener
    }
}