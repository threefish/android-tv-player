package com.tvplayer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.core.model.HomeGridItem
import com.tvplayer.core.repository.HomeGridRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeGridRepository: HomeGridRepository
) : ViewModel() {

    private val _gridItems = MutableStateFlow<List<HomeGridItem>>(emptyList())
    val gridItems: StateFlow<List<HomeGridItem>> = _gridItems.asStateFlow()

    init {
        viewModelScope.launch {
            homeGridRepository.getAllFlow().collect { items ->
                _gridItems.value = items
            }
        }
    }

    fun removeItem(id: Long) {
        viewModelScope.launch {
            homeGridRepository.remove(id)
        }
    }
}
