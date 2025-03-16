package ru.cloudstorage.client.ui.storage;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import ru.cloudstorage.client.rest.cloudstorage.Storage;

public class StorageViewModel extends ViewModel {
    private MutableLiveData<Storage> storage;

    public StorageViewModel() {
        storage = new MutableLiveData<>();
        storage.setValue(new Storage());
    }

    public LiveData<Storage> getStorage() {
        return storage;
    }
    public void setStorage(Storage storage) {
        this.storage.setValue(storage);
    }
}