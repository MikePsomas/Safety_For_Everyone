package com.example.myapplication



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ContactAdapter(private val contactList: MutableList<String>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    // Inner ViewHolder class
    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactTextView: TextView = itemView.findViewById(R.id.contactTextView)
    }
    // A method to inflate the layout for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(itemView)
    }
    // method to bind the data to each item view
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.contactTextView.text = contact
    }
    // method to return the total number of items in the list
    override fun getItemCount(): Int {
        return contactList.size
    }
    // Method to clear all items in the list
    fun clearItems() {
        contactList.clear()
        // Notify the adapter that data set has changed
        notifyDataSetChanged()

    }
}

