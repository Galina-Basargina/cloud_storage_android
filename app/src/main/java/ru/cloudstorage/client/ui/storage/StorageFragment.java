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
    private StorageAdapter adapter;
    private float startX;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("!!!2", this.toString());
//        Log.d("!!!2", this.getTag());
        Log.d("!!!2", container.toString());
        //Log.d("!!!2", this.container.toString());

        this.container = container;

        // ViewModel создается однократно, указатель на нее сохраняется во фрагменте
        // Таким образом, this.storageViewModel в этом месте всегда null, но после
        // инициализации он получает указатель на ранее созданный ViewModel объект
        storageViewModel = new ViewModelProvider(this).get(StorageViewModel.class);

        binding = FragmentStorageBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Настройка адаптера
        adapter = new StorageAdapter(getContext(), storageViewModel.getData().getValue());
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

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(getContext())
            .setTitle("Удаление элемента " + storageViewModel.getDataAt(position))
            .setMessage("Вы уверены?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                storageViewModel.removeDataAt(position);
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
                break;
        }
        return false;
    }

    @Override
    public void onRefresh() {
        //new Handler().postDelayed(() -> {
            SimpleService.getStorageData(this);
            storageViewModel.setDataAt(1, "Item " + System.currentTimeMillis());
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
    public void onAuthError(boolean resetToken) {
        storageViewModel.setDataAt(0, getResources().getString(R.string.error_unauthorized));
        if (resetToken)
            DatabasePreferences.getInstance().resetToken();
        //TODO: enableLogin();
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onNetworkError(String error) {
        storageViewModel.setDataAt(0, error);
        //TODO: enableLogin();
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoadStorageFailure() {
        // любая ошибка, кроме authError и networkError
        // По сути, это ошибки программиста (изменился интерфейс запросов или ответов)
        storageViewModel.setDataAt(0, "onLoadStorageFailure");
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoadStorageSuccess(Storage storage) {
        storageViewModel.setDataAt(0, "onLoadStorageSuccess");
        // Старая версия storage затирается на новую версию storage
        // Автоматически будут обновлены все данные с помощью observer
        storageViewModel.setStorage(storage);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }
}