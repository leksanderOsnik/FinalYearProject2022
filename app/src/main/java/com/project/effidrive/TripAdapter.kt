package com.project.effidrive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.effidrive.databinding.CardJourneySummaryBinding

class TripAdapter(
    private val mDataList: ArrayList<TripModel>,
    private val clickListener: TripClickListener
    ) : RecyclerView.Adapter<TripViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val from = LayoutInflater.from(parent.context)
        val binding = CardJourneySummaryBinding.inflate(from, parent, false)
        return TripViewHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bindTrip(mDataList[position])
    }

    override fun getItemCount(): Int = mDataList.size


}