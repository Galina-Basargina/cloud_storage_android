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

import ru.cloudstorage.client.R;
import ru.cloudstorage.client.rest.SimpleService;
import ru.cloudstorage.client.databinding.FragmentLoginBinding;
import ru.cloudstorage.client.db.DatabasePreferences;

public class LoginFragment extends Fragment implements LoginCallback {
    private LoginViewModel loginViewModel;
    private FragmentLoginBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Подключение модели к view
        final TextView textTitle = binding.textTitle;
        loginViewModel.getTitle().observe(getViewLifecycleOwner(), textTitle::setText);
        // ---
        final TextView editLogin = binding.editLogin;
        // ---
        final Button btnLogin = binding.btnLogin;
        loginViewModel.getEnter().observe(getViewLifecycleOwner(), btnLogin::setText);
        // ---
        final TextView textError = binding.textError;
        loginViewModel.getError().observe(getViewLifecycleOwner(), textError::setText);

        // Настройка компонентов
        final String login = DatabasePreferences.getInstance().getLogin();
        if (login != null)
            editLogin.setText(login);
        // ---
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

        DatabasePreferences.getInstance().setLogin(user);

        SimpleService.login(this);
    }

    @Override
    public void authError(boolean resetToken) {
        this.loginViewModel.notifyError(getResources().getString(R.string.error_unauthorized));
        if (resetToken)
            DatabasePreferences.getInstance().resetToken();
    }

    @Override
    public void networkError(String error) {
        this.loginViewModel.notifyError(error);
    }

    @Override
    public void onSuccess(String token) {
        Log.d("!!!", token);
        DatabasePreferences.getInstance().setToken(token);
    }
}