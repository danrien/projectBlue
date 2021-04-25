package com.lasthopesoftware.bluewater.shared.android.adapters

import android.content.Context
import android.os.Handler
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise

abstract class DeferredListAdapter<T, ViewHolder : RecyclerView.ViewHolder?>(
	context: Context,
	diffCallback: DiffUtil.ItemCallback<T>)
	: ListAdapter<T, ViewHolder>(diffCallback) {

	private val handler = lazy { Handler(context.mainLooper) }

	@Volatile
	private var currentUpdate: Promise<Unit> = Promise.empty()

	@Synchronized
	fun updateListEventually(list: List<T>): Promise<Unit> =
		currentUpdate
			.eventually({ promiseListUpdate(list) }, { promiseListUpdate(list) })
			.also { currentUpdate = it }

	private fun promiseListUpdate(list: List<T>) : Promise<Unit> = LoopedInPromise(MessengerOperator{
		try {
			submitList(list) { it.sendResolution(Unit) }
		} catch (e: Throwable) {
			it.sendRejection(e)
		}
	}, handler.value)
}
