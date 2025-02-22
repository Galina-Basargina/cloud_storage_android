package ru.cloudstorage.client.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;

import ru.cloudstorage.client.SimpleService;
import ru.cloudstorage.client.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        LoginViewModel loginViewModel =
                new ViewModelProvider(this).get(LoginViewModel.class);

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textTitle = binding.textTitle;
        loginViewModel.getTitle().observe(getViewLifecycleOwner(), textTitle::setText);

        final Button btnLogin = binding.btnLogin;
        loginViewModel.getEnter().observe(getViewLifecycleOwner(), btnLogin::setText);
        btnLogin.setOnClickListener(this::onLogin);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void onLogin(View view) {
        String user = binding.editLogin.getText().toString();
        String password = binding.editPassword.getText().toString();
        Log.d("!!!",user + " " + password);

        SimpleService.foo();
    }
}