package org.koitharu.kotatsu.list.ui.preview

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.core.util.ext.sanitize
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val extraProvider: ListExtraProvider,
	private val repositoryFactory: MangaRepository.Factory,
	private val imageGetter: Html.ImageGetter,
) : BaseViewModel() {

	val manga = MutableStateFlow(
		savedStateHandle.require<ParcelableManga>(MangaIntent.KEY_MANGA).manga,
	)

	val description = manga
		.distinctUntilChangedBy { it.description.orEmpty() }
		.transformLatest {
			val description = it.description
			if (description.isNullOrEmpty()) {
				emit(null)
			} else {
				emit(description.parseAsHtml().filterSpans().sanitize())
				emit(description.parseAsHtml(imageGetter = imageGetter).filterSpans())
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), null)

	val tagsChips = manga.map {
		it.tags.map { tag ->
			ChipsView.ChipModel(
				title = tag.title,
				tint = extraProvider.getTagTint(tag),
				icon = 0,
				data = tag,
				isCheckable = false,
				isChecked = false,
			)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	init {
		launchLoadingJob(Dispatchers.Default) {
			val repo = repositoryFactory.create(manga.value.source)
			manga.value = repo.getDetails(manga.value)
		}
	}

	private fun Spanned.filterSpans(): CharSequence {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable.trim()
	}
}
