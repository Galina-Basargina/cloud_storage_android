package ru.cloudstorage.client.ui.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
    private ContextMenu contextMenu;
    private boolean errorInData;

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
        // Подключаем метод свайпа для удаления и отслеживания нажатий на списке
        listView.setOnTouchListener(this);
        // Подключаем метод свайпа для обновления
        swipeRefreshLayout.setOnRefreshListener(this);

        // Подключаем возможность использования меню во фрагменте
        errorInData = true;
        setHasOptionsMenu(true);
        // Подключаем контекстное меню для определенного элемента
        registerForContextMenu(binding.listView);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
        contextMenu = menu;

        contextMenu.findItem(R.id.action_context_delete).setEnabled(!errorInData);
        contextMenu.findItem(R.id.action_context_open).setEnabled(!errorInData);
        contextMenu.findItem(R.id.action_context_rename).setEnabled(!errorInData);
        contextMenu.findItem(R.id.action_context_replace).setEnabled(!errorInData);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.d("!!!", String.valueOf(info.position));
        if (item.getItemId() == R.id.action_context_delete) {
            dialogDelete(info.position);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void notifyError(String error) {
        data_TO_BE_DELETED.clear();
        data_TO_BE_DELETED.add(error);
        adapter.notifyDataSetChanged();
        errorInData = true;
    }

    private void dialogDelete(int position) {
        new AlertDialog.Builder(getContext())
            .setTitle("Удаление элемента " + data_TO_BE_DELETED.get(position))
            .setMessage("Вы уверены?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                doDelete(position);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private boolean longPress = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final GestureDetector gestureDetector = new GestureDetector(
            getContext(),
            new GestureDetector.SimpleOnGestureListener() {
                public void onLongPress(MotionEvent e) {
                    longPress = true;
                }
            }
        );

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                longPress = false;
                return gestureDetector.onTouchEvent(event);

            case MotionEvent.ACTION_UP:
                if (longPress) return false;
                float endX = event.getX();
                if (Math.abs(startX - endX) > 100) {
                    // Определение свайпа
                    // см. пример https://github.com/Galina-Basargina/swipe_view_app
                    int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    if (position != AdapterView.INVALID_POSITION) {
                        dialogDelete(position);
                    }
                }
                else {
                    // Выбор папки или файла
                    int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                    if (position != AdapterView.INVALID_POSITION) {
                        Folder folder = data_TO_BE_DELETED.getFolderByPosition(position);
                        if (folder != null) {
                            Log.d("!!!folder", folder.getId().toString());
                            Storage storage = storageViewModel.getStorage().getValue();
                            if (storage != null) {
                                storage.setCurrentFolder(folder);
                                selectCurrentFolder(storage);
                            }
                        }
                        else {
                            File file = data_TO_BE_DELETED.getFileByPosition(position);
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

    private void doDelete(int position) {
        File file = data_TO_BE_DELETED.getFileByPosition(position);
        if (file != null) {
            SimpleService.getInstance().removeFileAndGetStorageData(this);
            return;
        }

        Folder folder = data_TO_BE_DELETED.getFolderByPosition(position);
        if (folder != null) {
            SimpleService.getInstance().removeFolderAndGetStorageData(this);
        }
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
        errorInData = false;
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