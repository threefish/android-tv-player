package com.tvplayer.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _trashFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val trashFiles: StateFlow<List<FileInfo>> = _trashFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTrash()
    }

    fun loadTrash() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trashFiles.value = fileRepository.getTrash()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreFile(path: String) {
        viewModelScope.launch {
            try {
                fileRepository.restore(path)
                loadTrash()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
