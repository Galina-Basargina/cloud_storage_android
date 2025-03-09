package ru.cloudstorage.client.ui.login;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import ru.cloudstorage.client.R;
import ru.cloudstorage.client.databinding.FragmentLoginBinding;
import ru.cloudstorage.client.db.DatabasePreferences;
import ru.cloudstorage.client.rest.SimpleService;
import ru.cloudstorage.client.ui.storage.StorageFragment;

public class LoginFragment extends Fragment implements LoginCallback {
    private LoginViewModel loginViewModel;
    private FragmentLoginBinding binding;
    private ViewGroup container;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("!!!1", this.toString());
        Log.d("!!!1", this.getTag());

        this.container = container;
        Log.d("!!!1", this.container.toString());

        this.loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Подключение модели к view
        final TextView textTitle = binding.textTitle;
        loginViewModel.getTitle().observe(getViewLifecycleOwner(), textTitle::setText);
        // ---
        final EditText editLogin = binding.editLogin;
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

        // Включение режима входа
        if (DatabasePreferences.getInstance().getToken() == null)
            enableLogin();
        else
            disableLogin();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onLogin(View view) {
        loginViewModel.resetError();

        String user = binding.editLogin.getText().toString();
        String password = binding.editPassword.getText().toString();

        DatabasePreferences.getInstance().setLogin(user);

        SimpleService.login(this, user, password);
    }

    private void onLogout(View view) {
        loginViewModel.resetError();
        enableLogin();

        DatabasePreferences.getInstance().resetToken();

        SimpleService.logout();
    }

    @Override
    public void authError(boolean resetToken) {
        loginViewModel.notifyError(getResources().getString(R.string.error_unauthorized));
        if (resetToken)
            DatabasePreferences.getInstance().resetToken();
        enableLogin();
    }

    @Override
    public void networkError(String error) {
        this.loginViewModel.notifyError(error);
        enableLogin();
    }

    @Override
    public void onSuccess(String token) {
        Log.d("!!!", token);
        DatabasePreferences.getInstance().setToken(token);
        disableLogin();

        Fragment fragment = getFragmentManager().findFragmentById(this.getId());
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                //.replace(((ViewGroup)getView().getParent()).getId(), StorageFragment.class, null)
                .replace(this.container.getId(), StorageFragment.class, null)
                .setReorderingAllowed(true)
                .addToBackStack(null) // Name can be null
                .commit();


//        FragmentManager manager = getActivity().getSupportFragmentManager();
//        manager.popBackStack(R.id.nav_storage, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        //Fragment fragment = manager.findFragmentById(R.id.nav_storage); // new StorageFragment();
//        Fragment fragment = new StorageFragment();
//        FragmentTransaction transaction = manager.beginTransaction();
//        transaction.replace(R.id.nav_host_fragment_content_main, fragment);
//        transaction.commit();

//        Fragment fragment = getFragmentManager().findFragmentById(this.getId());
////        Fragment fragment2 = fragment.getParentFragmentManager().getFragments().get(1);
//        Log.d("!!!", fragment.toString());
//        Log.d("!!!", fragment.getTag());
////        Log.d("!!!", fragment2.toString());
////        Log.d("!!!", fragment2.getTag());
////        Log.d("!!!", String.valueOf(fragment2.getParentFragment().getId()));
//
////        final FragmentTransaction ft = getFragmentManager().beginTransaction();
////        ft.show()
////        replace(fragment.getParentFragment().getId(), new StorageFragment());
////        ft.commit();
    }

    private void disableEditText(EditText editText) {
        //editText.setFocusable(false);
        editText.setEnabled(false);
        //editText.setCursorVisible(false);
        //editText.setKeyListener(null);
        //editText.setBackgroundColor(Color.TRANSPARENT);
    }

    private void enableEditText(EditText editText) {
        //editText.setFocusable(true);
        editText.setEnabled(true);
        //editText.setCursorVisible(true);
        //editText.setKeyListener(null);
        //editText.setBackgroundColor(Color.TRANSPARENT);
    }

    private void enableLogin() {
        binding.btnLogin.setOnClickListener(this::onLogin);
        enableEditText(binding.editLogin);
        enableEditText(binding.editPassword);
        loginViewModel.setEnter(getResources().getString(R.string.button_login));
    }

    private void disableLogin() {
        binding.btnLogin.setOnClickListener(this::onLogout);
        disableEditText(binding.editLogin);
        disableEditText(binding.editPassword);
        loginViewModel.setEnter(getResources().getString(R.string.button_logout));
    }
}