package com.namehillsoftware.projectblue.shared.promises.extensions

import com.namehillsoftware.handoff.promises.MessengerOperator
import com.vedsoft.futures.runnables.OneParameterAction
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

open class ProgressingPromise<Progress, Resolution> : ProgressedPromise<Progress, Resolution> {
	private val updateListeners = ConcurrentLinkedQueue<OneParameterAction<Progress>>()
	private val atomicProgress: AtomicReference<Progress?> = AtomicReference()
	private var isResolved = false

	constructor(resolution: Resolution?) : super(resolution)
	constructor(messengerOperator: MessengerOperator<Resolution>?) : super(messengerOperator)
	protected constructor()

	override val progress: Progress?
		get() {
			return atomicProgress.get()
		}

	protected fun reportProgress(progress: Progress) {
		atomicProgress.lazySet(progress)
		for (action in updateListeners) action.runWith(progress)
	}

	fun updates(action: OneParameterAction<Progress>): ProgressingPromise<Progress, Resolution> {
		val currentProgress = atomicProgress.get()
		if (currentProgress != null)
			action.runWith(currentProgress)

		if (!isResolved) {
			updateListeners.add(action)
			must {
				isResolved = true
				updateListeners.remove(action)
			}
		}

		return this
	}

	protected fun proxy(source: ProgressingPromise<Progress, Resolution>): ProgressingPromise<Progress, Resolution> {
		source
			.updates(OneParameterAction { reportProgress(it) })
			.then({resolve(it)}, {reject(it)})

		return this
	}
}
