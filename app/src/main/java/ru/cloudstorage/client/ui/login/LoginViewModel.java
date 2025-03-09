package ru.cloudstorage.client.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<String> title;
    private final MutableLiveData<String> enter;
    private final MutableLiveData<String> error;

    public LoginViewModel() {
        title = new MutableLiveData<>();
        title.setValue("Вход в систему");
        enter = new MutableLiveData<>();
        enter.setValue(null);
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
    public void setEnter(String enter) {
        this.enter.setValue(enter);
    }
    public void resetError() {
        this.error.setValue(null);
    }
}