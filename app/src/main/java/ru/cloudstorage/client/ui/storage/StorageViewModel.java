package ru.cloudstorage.client.ui.storage;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class StorageViewModel extends ViewModel {
    private MutableLiveData<List<String>> data;

    public StorageViewModel() {
        Log.d("!!!4", this.toString());

        data = new MutableLiveData<>();
        data.setValue(new ArrayList<String>());

        // Инициализация данных
        for (int i = 1; i <= 20; i++) {
            data.getValue().add("Item " + i);
        }
    }

    public LiveData<List<String>> getData() {
        return data;
    }
    public String getDataAt(int position) {
        return data.getValue().get(position);
    }
    public void setDataAt(int position, String value) {
        data.getValue().set(position, value);
    }
    public void removeDataAt(int position) {
        data.getValue().remove(position);
    }
}