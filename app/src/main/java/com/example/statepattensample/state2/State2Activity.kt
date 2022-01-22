package com.example.statepattensample.state2

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.statepattensample.StateActivity
import com.example.statepattensample.state2.State2ViewModel.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class State2Activity : StateActivity() {

    private val viewModel: State2ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.loading.isVisible = state is State.Loading
                binding.error.isVisible = state is State.Failure
                binding.card.isVisible = state is State.Success

                if (state is State.Success) {
                    binding.name.text = state.name
                    binding.age.text = state.age
                }
            }
        }
    }
}
