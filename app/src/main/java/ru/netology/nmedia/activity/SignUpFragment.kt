package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.SignUpViewModel
import kotlin.getValue

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(
            inflater,
            container,
            false
        )
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.error) {
                Toast.makeText(requireContext(), R.string.registration_error, Toast.LENGTH_SHORT)
                    .show()
            }
            if (state.successfully) {
                findNavController().navigateUp()
            }
        }
        binding.signUp.setOnClickListener {
            val name = binding.name.text.toString()
            val login = binding.login.text.toString()
            val password = binding.password.text.toString()
            val password_conf = binding.passwConf.text.toString()
            if (password != password_conf) {
                Toast.makeText(requireContext(), R.string.passw_dont_match, Toast.LENGTH_SHORT)
                    .show()
            } else {
                viewModel.signUp(name, login, password)
            }
        }
        return binding.root
    }
}