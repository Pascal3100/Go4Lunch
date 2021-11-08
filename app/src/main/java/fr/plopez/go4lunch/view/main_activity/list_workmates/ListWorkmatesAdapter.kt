package fr.plopez.go4lunch.view.main_activity.list_workmates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.view.model.WorkmateViewState

class ListWorkmatesAdapter :
    ListAdapter<WorkmateViewState, ListWorkmatesAdapter.WorkmateViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        WorkmateViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.workmates_list_view_item, parent, false
            )
        )

    override fun onBindViewHolder(workmateViewHolder: WorkmateViewHolder, position: Int) {
        workmateViewHolder.bind(currentList[position])
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    private class DiffCallback : DiffUtil.ItemCallback<WorkmateViewState>() {

        override fun areItemsTheSame(oldItem: WorkmateViewState, newItem: WorkmateViewState) =
            oldItem.hashCode() == newItem.hashCode()

        override fun areContentsTheSame(oldItem: WorkmateViewState, newItem: WorkmateViewState) =
            oldItem == newItem
    }

    class WorkmateViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val photo: ImageView = itemView.findViewById(R.id.wlv_item_workmate_avatar)
        private val text: TextView = itemView.findViewById(R.id.wlv_item_workmate_status_text)

        fun bind(workmateViewState: WorkmateViewState){
            Glide.with(itemView.context)
                .load(workmateViewState.photoUrl)
                .placeholder(R.drawable.ic_no_profile_photo_available)
                .error(R.drawable.ic_no_profile_photo_available)
                .fallback(R.drawable.ic_no_profile_photo_available)
                .circleCrop()
                .into(photo)

            text.text = workmateViewState.text
        }
    }
}