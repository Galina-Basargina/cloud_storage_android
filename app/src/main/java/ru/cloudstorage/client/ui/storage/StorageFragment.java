package ru.cloudstorage.client.ui.storage;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import ru.cloudstorage.client.databinding.FragmentStorageBinding;

public class StorageFragment extends Fragment
        implements View.OnTouchListener,
                   SwipeRefreshLayout.OnRefreshListener {
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
            storageViewModel.setDataAt(1, "Item " + System.currentTimeMillis());
            adapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        //}, 1000);
    }
}