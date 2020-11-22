package com.namehillsoftware.projectblue.shared.android.view

import androidx.annotation.IdRes

class MissingViewException(@param:IdRes private val viewId: Int) : Exception("View $viewId could not be found")
