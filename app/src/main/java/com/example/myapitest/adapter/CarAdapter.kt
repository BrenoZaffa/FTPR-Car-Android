package com.example.myapitest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meuprimeiroapp.ui.loadImage
import com.example.myapitest.R
import com.example.myapitest.model.CarValue
import com.squareup.picasso.Picasso

class CarAdapter(
    private val items: List<CarValue>,
    private val onClickItem: (CarValue) -> Unit
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {
    class CarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.image)
        val modelTextView = view.findViewById<TextView>(R.id.model)
        val yearTextView = view.findViewById<TextView>(R.id.year)
        val licenseTextView = view.findViewById<TextView>(R.id.license)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarAdapter.CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarAdapter.CarViewHolder, position: Int) {
        val item = items[position]
        holder.modelTextView.text = item.name
        holder.yearTextView.text = item.year
        holder.licenseTextView.text = item.license
        holder.imageView.loadImage(item.imageUrl)
        holder.itemView.setOnClickListener {
            onClickItem(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
