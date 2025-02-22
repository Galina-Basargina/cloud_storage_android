package ru.cloudstorage.client.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<String> mTitle;
    private final MutableLiveData<String> mEnter;

    public LoginViewModel() {
        mTitle = new MutableLiveData<>();
        mTitle.setValue("Вход в систему");
        mEnter = new MutableLiveData<>();
        mEnter.setValue("Войти");
    }

    public LiveData<String> getTitle() {
        return mTitle;
    }
    public LiveData<String> getEnter() {
        return mEnter;
    }
}