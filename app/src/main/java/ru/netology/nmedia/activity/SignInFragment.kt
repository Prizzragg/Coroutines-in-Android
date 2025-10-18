package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.SignInViewModel

@AndroidEntryPoint
class SignInFragment : Fragment() {
    private val viewModel: SignInViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignInBinding.inflate(
            inflater,
            container,
            false
        )

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.error) {
                Toast.makeText(requireContext(), R.string.auth_error, Toast.LENGTH_SHORT)
                    .show()
            }
            if (state.successfully) {
                findNavController().navigateUp()
            }
        }
        binding.signIn.setOnClickListener {
            val login = binding.login.text.toString()
            val password = binding.password.text.toString()
            viewModel.signIn(login, password)
        }
        return binding.root
    }
}