package com.bitpunchlab.android.pawsgo.messages

import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.pawsgo.R
import com.bitpunchlab.android.pawsgo.base.BaseRecyclerViewAdapter
import com.bitpunchlab.android.pawsgo.base.GenericListener
import com.bitpunchlab.android.pawsgo.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.pawsgo.databinding.ItemMessageBinding
import com.bitpunchlab.android.pawsgo.modelsRoom.MessageRoom

class MessagesAdapter(var clickListener: MessageOnClickListener) : BaseRecyclerViewAdapter<MessageRoom>(
    clickListener = clickListener,
    compareItems = { old, new ->  old.messageID == new.messageID },
    compareContents = { old, new ->  old.date == new.date },
    bindingInter = object : GenericRecyclerBindingInterface<MessageRoom> {
        override fun bindData(
            item: MessageRoom,
            binding: ViewDataBinding,
            onClickListener: GenericListener<MessageRoom>?,
            messageClickListener: GenericListener<MessageRoom>?
        ) {
            (binding as ItemMessageBinding).message = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

    }
) {
    override fun getLayoutRes(viewType: Int): Int {
        return R.layout.activity_main
    }
}

class MessageOnClickListener(override val clickListener: (MessageRoom) -> Unit) :
    GenericListener<MessageRoom>(clickListener) {

        fun onClick(message: MessageRoom) = clickListener(message)
}