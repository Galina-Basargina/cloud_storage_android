package ru.cloudstorage.client.ui.storage;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import ru.cloudstorage.client.R;
import ru.cloudstorage.client.databinding.FragmentStorageBinding;
import ru.cloudstorage.client.db.DatabasePreferences;
import ru.cloudstorage.client.rest.SimpleService;
import ru.cloudstorage.client.rest.cloudstorage.File;
import ru.cloudstorage.client.rest.cloudstorage.Folder;
import ru.cloudstorage.client.rest.cloudstorage.Storage;
import ru.cloudstorage.client.rest.cloudstorage.User;

public class StorageFragment extends Fragment implements
        View.OnTouchListener,
        SwipeRefreshLayout.OnRefreshListener,
        StorageCallback {
    private ViewGroup container;
    StorageViewModel storageViewModel;
    private FragmentStorageBinding binding;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StorageItems data_TO_BE_DELETED;
    private StorageAdapter adapter;
    private float startX;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;

        // ViewModel создается однократно, указатель на нее сохраняется во фрагменте
        // Таким образом, this.storageViewModel в этом месте всегда null, но после
        // инициализации он получает указатель на ранее созданный ViewModel объект
        storageViewModel = new ViewModelProvider(this).get(StorageViewModel.class);

        binding = FragmentStorageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация данных
        data_TO_BE_DELETED = new StorageItems();

        // Настройка адаптера
        adapter = new StorageAdapter(getContext(), data_TO_BE_DELETED);
        listView = binding.listView;
        swipeRefreshLayout = binding.refreshLayout;
        // Подключаем адаптер
        listView.setAdapter(adapter);
        // Подключаем метод свайпа для удаления
        listView.setOnTouchListener(this);
        // Подключаем метод свайпа для обновления
        swipeRefreshLayout.setOnRefreshListener(this);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void notifyError(String error) {
        data_TO_BE_DELETED.clear();
        data_TO_BE_DELETED.add(error);
        adapter.notifyDataSetChanged();
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(getContext())
            .setTitle("Удаление элемента " + data_TO_BE_DELETED.get(position))
            .setMessage("Вы уверены?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                data_TO_BE_DELETED.remove(position);
                adapter.notifyDataSetChanged();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float endX = event.getX();
                if (Math.abs(startX - endX) > 100) {
                    // Определение свайпа
                    // см. пример https://github.com/Galina-Basargina/swipe_view_app
                    int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    if (position != AdapterView.INVALID_POSITION) {
                        showDeleteDialog(position);
                    }
                }
                else {
                    // Выбор папки или файла
                    int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    if (position != AdapterView.INVALID_POSITION) {
                        String name = data_TO_BE_DELETED.get(position);
                        Folder folder = data_TO_BE_DELETED.getFolder(name);
                        if (folder != null) {
                            Log.d("!!!folder", folder.getId().toString());
                            Storage storage = storageViewModel.getStorage().getValue();
                            if (storage != null) {
                                storage.setCurrentFolder(folder);
                                selectCurrentFolder(storage);
                            }
                        }
                        else {
                            File file = data_TO_BE_DELETED.getFile(name);
                            if (file != null)
                                Log.d("!!!file", file.getId().toString());
                            else
                                Log.d("!!!", "error");
                        }
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void onRefresh() {
        //new Handler().postDelayed(() -> {
            SimpleService.getInstance().getStorageData(this);
            // Далее будет работать ассинхронный метод getStorageData
            // Его работа завершается вызовом методов StorageCallback, поэтому именно там
            // будет обновляться набор данных адаптера и останавливаться иконка "крутилки"
        //}, 1000);
    }

    @Override
    public String getToken() {
        return DatabasePreferences.getInstance().getToken();
    }

    @Override
    public User getUser() {
        return storageViewModel.getStorage().getValue().getUser();
    }

    @Override
    public Folder getCurrentFolder() {
        return storageViewModel.getStorage().getValue().getCurrentFolder();
    }

    @Override
    public void onAuthError(boolean resetToken) {
        if (resetToken)
            DatabasePreferences.getInstance().resetToken();
        //TODO: enableLogin();
        notifyError(getResources().getString(R.string.error_unauthorized));
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onNetworkError(String error) {
        //TODO: enableLogin();
        notifyError(error);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoadStorageFailure() {
        // любая ошибка, кроме authError и networkError
        // По сути, это ошибки программиста (изменился интерфейс запросов или ответов)
        notifyError("onLoadStorageFailure");
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoadStorageSuccess(Storage storage) {
        selectCurrentFolder(storage);

        // Старая версия storage затирается на новую версию storage
        // Автоматически будут обновлены все данные с помощью observer
        storageViewModel.setStorage(storage);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void selectCurrentFolder(Storage storage) {
        data_TO_BE_DELETED.clear();
        if (storage.getCurrentFolder().getParent() != null) {
            for (Folder folder : storage.getFolders())
                if (folder.getId().intValue() == storage.getCurrentFolder().getParent().intValue()) {
                    data_TO_BE_DELETED.addParentFolder(folder);
                    break;
                }
        }
        for (Folder f: storage.getFolders()) {
            if (f.getParent() == null)
                continue;
            if (f.getParent().intValue() == storage.getCurrentFolder().getId().intValue())
                data_TO_BE_DELETED.addFolder(f);
        }
        for (File f: storage.getFiles()) {
            if (f.getFolder().intValue() == storage.getCurrentFolder().getId().intValue())
                data_TO_BE_DELETED.addFile(f);
        }
        adapter.notifyDataSetChanged();
    }
}