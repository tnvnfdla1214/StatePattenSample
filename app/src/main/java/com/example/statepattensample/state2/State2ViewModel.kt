package com.example.statepattensample.state2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statepattensample.data.UserRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class State2ViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    sealed class State {

        data class Success(
            val name: String,
            val age: String
        ) : State()

        object Empty : State()

        object Failure : State()

        object Loading : State()
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()


    init {
        viewModelScope.launch {
            _state.value = State.Loading
            val result = userRepository.getUser()
            result
                .onSuccess { user ->
                    val (name, age) = (user.name to user.age.toString())
                    if (name.isEmpty() && age.isEmpty()) {
                        _state.value = State.Empty
                    } else {
                        _state.value = State.Success(user.name, user.age.toString())
                    }
                }
                .onFailure {
                    _state.value = State.Failure
                }
        }
    }
}
