package com.lasthopesoftware.bluewater.client.browsing.items.menu

abstract class AbstractViewChangedListenerContainer {
	private var onViewChangedListener: OnViewChangedListener? = null

	protected fun getOnViewChangedListener(): OnViewChangedListener? {
		return onViewChangedListener
	}

	fun setOnViewChangedListener(onViewChangedListener: OnViewChangedListener?) {
		this.onViewChangedListener = onViewChangedListener
	}
}
