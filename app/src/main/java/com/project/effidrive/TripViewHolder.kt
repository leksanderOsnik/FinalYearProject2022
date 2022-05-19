package com.project.effidrive

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.project.effidrive.databinding.CardJourneySummaryBinding

class TripViewHolder(
    private val cardJourneySummaryBinding: CardJourneySummaryBinding,
    private val clickListener: TripClickListener
): RecyclerView.ViewHolder(cardJourneySummaryBinding.root) {



    fun bindTrip(trip: TripModel){
        cardJourneySummaryBinding.tvDateContent.text = trip.date
        cardJourneySummaryBinding.tvOrigin.text = trip.origin
        cardJourneySummaryBinding.tvDestination.text = trip.destination
        cardJourneySummaryBinding.tvRating.text = trip.rating
        cardJourneySummaryBinding.tvDistanceContent.text = trip.distance
        cardJourneySummaryBinding.tvDurationContent.text = trip.duration
        cardJourneySummaryBinding.tvTripId.text = trip.tripId

        cardJourneySummaryBinding.cardJourneySummary.setOnClickListener{
            clickListener.onClick(trip)
        }
    }

}