package ru.cloudstorage.client.ui.storage;

import ru.cloudstorage.client.rest.cloudstorage.Storage;
import ru.cloudstorage.client.rest.cloudstorage.User;

public interface StorageCallback {
    String getToken();
    User getUser();

    void onAuthError(boolean resetToken);
    void onNetworkError(String error);
    void onLoadStorageFailure();

    void onLoadStorageSuccess(Storage storage);
}
