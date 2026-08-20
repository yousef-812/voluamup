package com.bdaey.voluamup.ui.calllog

import android.graphics.Color
import android.provider.CallLog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
            val formattedDate = DateFormat.format("dd/MM  HH:mm", Date(item.date)).toString()

            // ── Call type label + icon + color ────────────────────────────
            val (typeStr, iconRes, iconColor) = when (item.type) {
                CallLog.Calls.INCOMING_TYPE ->
                    Triple(
                        context.getString(R.string.type_incoming),
                        android.R.drawable.sym_call_incoming,
                        ContextCompat.getColor(context, R.color.call_green)
                    )
                CallLog.Calls.OUTGOING_TYPE ->
                    Triple(
                        context.getString(R.string.type_outgoing),
                        android.R.drawable.sym_call_outgoing,
                        ContextCompat.getColor(context, R.color.primary)
                    )
                CallLog.Calls.MISSED_TYPE ->
                    Triple(
                        context.getString(R.string.type_missed),
                        android.R.drawable.sym_call_missed,
                        ContextCompat.getColor(context, R.color.call_red)
                    )
                CallLog.Calls.REJECTED_TYPE ->
                    Triple(
                        context.getString(R.string.type_rejected),
                        android.R.drawable.sym_call_missed,
                        ContextCompat.getColor(context, R.color.call_red)
                    )
                CallLog.Calls.BLOCKED_TYPE ->
                    Triple(
                        context.getString(R.string.type_blocked),
                        android.R.drawable.ic_delete,
                        Color.GRAY
                    )
                CallLog.Calls.ANSWERED_EXTERNALLY_TYPE ->
                    Triple(
                        context.getString(R.string.type_answered_externally),
                        android.R.drawable.sym_call_incoming,
                        ContextCompat.getColor(context, R.color.call_green)
                    )
                else ->
                    Triple(
                        "",
                        android.R.drawable.sym_call_incoming,
                        ContextCompat.getColor(context, R.color.text_secondary)
                    )
            }

            // ── Duration (only for answered calls) ────────────────────────
            val durationStr = if (item.duration > 0) {
                val mins = item.duration / 60
                val secs = item.duration % 60
                if (mins > 0) " • ${mins}m ${secs}s" else " • ${secs}s"
            } else ""

            // ── Bind text ─────────────────────────────────────────────────
            if (!item.name.isNullOrEmpty()) {
                binding.tvLogNameOrNumber.text = item.name
                val subtitle = buildString {
                    append(item.number)
                    if (typeStr.isNotEmpty()) append(" • $typeStr")
                    append(durationStr)
                    append(" • $formattedDate")
                }
                binding.tvLogDateAndType.text = subtitle
            } else {
                binding.tvLogNameOrNumber.text = item.number
                val subtitle = buildString {
                    if (typeStr.isNotEmpty()) append(typeStr)
                    append(durationStr)
                    append(" • $formattedDate")
                }
                binding.tvLogDateAndType.text = subtitle
            }

            // Color the name red for missed/rejected calls to make them stand out
            val nameColor = when (item.type) {
                CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE ->
                    ContextCompat.getColor(context, R.color.call_red)
                else ->
                    ContextCompat.getColor(context, R.color.text_primary)
            }
            binding.tvLogNameOrNumber.setTextColor(nameColor)

            // ── Bind icon ─────────────────────────────────────────────────
            binding.ivCallType.setImageResource(iconRes)
            binding.ivCallType.setColorFilter(iconColor)

            // ── Bind call button ──────────────────────────────────────────
            binding.btnCallLog.setOnClickListener {
                onCallClick(item)
            }
        }
    }
}

