package com.bdaey.volumeup.ui.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bdaey.volumeup.databinding.ItemContactBinding
import com.bdaey.volumeup.model.Contact

class ContactsAdapter(
    private var contacts: List<Contact>,
    private val onCallClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    fun updateData(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = contact.phoneNumber

            if (!contact.emailAddress.isNullOrEmpty()) {
                binding.tvContactEmail.visibility = View.VISIBLE
                binding.tvContactEmail.text = contact.emailAddress
            } else {
                binding.tvContactEmail.visibility = View.GONE
            }

            binding.btnCallContact.setOnClickListener {
                onCallClick(contact)
            }
        }
    }
}
