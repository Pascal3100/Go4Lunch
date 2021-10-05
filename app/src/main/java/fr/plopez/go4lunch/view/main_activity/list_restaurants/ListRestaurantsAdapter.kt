package fr.plopez.go4lunch.view.main_activity.list_restaurants

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import fr.plopez.go4lunch.R
import fr.plopez.go4lunch.data.model.restaurant.RestaurantItemViewState

class ListRestaurantsAdapter : RecyclerView.Adapter<ListRestaurantsAdapter.RestaurantViewHolder>() {

    private val restaurantItemViewStateList = mutableListOf<RestaurantItemViewState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        return RestaurantViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_view_restaurant_recyclerview_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(restaurantViewHolder: RestaurantViewHolder, position: Int) {
        restaurantViewHolder.bind(restaurantItemViewStateList[position])
    }

    override fun getItemCount(): Int {
        return restaurantItemViewStateList.size
    }

    fun updateRestaurantsList(incomingRestaurantItemViewStateList: List<RestaurantItemViewState>) {
        restaurantItemViewStateList.clear()
        restaurantItemViewStateList.addAll(incomingRestaurantItemViewStateList)
        notifyDataSetChanged()
    }


    class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.lvr_item_restaurant_name)
        val distance: TextView = itemView.findViewById(R.id.lvr_item_restaurant_distance)
        val address: TextView = itemView.findViewById(R.id.lvr_item_restaurant_subtitle)
        val numberOfUsers: TextView =
            itemView.findViewById(R.id.lvr_item_restaurant_number_of_users)
        val openingText: TextView = itemView.findViewById(R.id.lvr_item_restaurant_opening)
        val ratingBar: RatingBar = itemView.findViewById(R.id.lvr_item_restaurant_rating_bar)
        val photo: ImageView = itemView.findViewById(R.id.lvr_item_restaurant_image)

        fun bind(restaurantItemViewState: RestaurantItemViewState) {
            name.text = restaurantItemViewState.name
            distance.text = restaurantItemViewState.distanceToUser
            address.text = restaurantItemViewState.address
            numberOfUsers.text = restaurantItemViewState.numberOfInterestedWorkmates
            openingText.text = restaurantItemViewState.openingStateText
            ratingBar.rating = restaurantItemViewState.rate

//            Log.d("TAG", "#### photoUrl: ${restaurantItemViewState.photoUrl}")
            // Glide section
            Glide.with(itemView.context)
                .load(restaurantItemViewState.photoUrl)
                .placeholder(R.drawable.no_pic_for_item_view)
                .error(R.drawable.no_pic_for_item_view)
                .fallback(R.drawable.no_pic_for_item_view)
                .centerCrop()
                .into(photo)
        }
    }


}