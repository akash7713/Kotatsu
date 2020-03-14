package org.koitharu.kotatsu.ui.settings.utils

import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.Preference

class EditTextSummaryProvider(@StringRes private val emptySummaryId: Int) :
	Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(preference: EditTextPreference): CharSequence {
		return if (preference.text.isNullOrEmpty()) {
			preference.context.getString(emptySummaryId)
		} else {
			preference.text
		}
	}
}