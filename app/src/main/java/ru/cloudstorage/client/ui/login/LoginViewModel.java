package ru.cloudstorage.client.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ru.cloudstorage.client.R;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<String> title;
    private final MutableLiveData<String> enter;
    private final MutableLiveData<String> error;

    public LoginViewModel() {
        title = new MutableLiveData<>();
        title.setValue("Вход в систему");
        enter = new MutableLiveData<>();
        enter.setValue("Войти");
        error = new MutableLiveData<>();
        error.setValue(null);
    }

    public LiveData<String> getTitle() {
        return title;
    }
    public LiveData<String> getEnter() {
        return enter;
    }
    public LiveData<String> getError() {
        return error;
    }
    public void notifyError(String error) {
        this.error.setValue(error);
    }
}