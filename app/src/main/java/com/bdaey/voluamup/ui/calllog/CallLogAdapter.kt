package com.bdaey.voluamup.ui.calllog

import android.provider.CallLog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bdaey.voluamup.R
import com.bdaey.voluamup.databinding.ItemCallLogBinding
import com.bdaey.voluamup.model.CallLogItem
import java.util.Date

class CallLogAdapter(
    private var logs: List<CallLogItem>,
    private val onCallClick: (CallLogItem) -> Unit
) : RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    fun updateData(newLogs: List<CallLogItem>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val binding = ItemCallLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CallLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    inner class CallLogViewHolder(private val binding: ItemCallLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CallLogItem) {
            val context = binding.root.context
            val formattedDate = DateFormat.format("dd/MM/yyyy HH:mm", Date(item.date))
            val typeStr = when (item.type) {
                CallLog.Calls.INCOMING_TYPE -> context.getString(R.string.type_incoming)
                CallLog.Calls.OUTGOING_TYPE -> context.getString(R.string.type_outgoing)
                CallLog.Calls.MISSED_TYPE -> context.getString(R.string.type_missed)
                else -> ""
            }

            if (!item.name.isNullOrEmpty()) {
                binding.tvLogNameOrNumber.text = item.name
                binding.tvLogDateAndType.text = "${item.number} • $typeStr • $formattedDate"
            } else {
                binding.tvLogNameOrNumber.text = item.number
                binding.tvLogDateAndType.text = "$typeStr • $formattedDate"
            }

            val iconRes = when (item.type) {
                CallLog.Calls.INCOMING_TYPE -> android.R.drawable.sym_call_incoming
                CallLog.Calls.OUTGOING_TYPE -> android.R.drawable.sym_call_outgoing
                else -> android.R.drawable.sym_call_missed
            }
            binding.ivCallType.setImageResource(iconRes)

            binding.btnCallLog.setOnClickListener {
                onCallClick(item)
            }
        }
    }
}
