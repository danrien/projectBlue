package com.namehillsoftware.projectblue.shared

class MagicPropertyBuilder(c: Class<*>) {
	private val canonicalName = c.canonicalName

	fun buildProperty(propertyName: String): String = buildMagicPropertyName(canonicalName, propertyName)

	companion object {
		@JvmStatic
		fun buildMagicPropertyName(c: Class<*>, propertyName: String): String =
			buildMagicPropertyName(c.canonicalName, propertyName)

		private fun buildMagicPropertyName(prefix: String?, propertyName: String): String =
			"$prefix.$propertyName"
	}
}
