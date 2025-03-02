package ru.cloudstorage.client.ui.login;

public interface LoginCallback {
    void authError(boolean resetToken);
    void networkError(String error);
    void onSuccess(String token);
}
