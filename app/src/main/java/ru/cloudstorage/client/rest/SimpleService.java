package ru.cloudstorage.client.rest;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import ru.cloudstorage.client.rest.cloudstorage.Auth;
import ru.cloudstorage.client.rest.cloudstorage.Folder;
import ru.cloudstorage.client.rest.cloudstorage.Logoff;
import ru.cloudstorage.client.rest.cloudstorage.Storage;
import ru.cloudstorage.client.rest.cloudstorage.User;
import ru.cloudstorage.client.ui.login.LoginCallback;
import ru.cloudstorage.client.ui.storage.StorageCallback;

public final class SimpleService {
    private static final String CLOUD_STORAGE_URL = "http://336707.simplecloud.ru";

    public interface CloudStorage {
        @Headers("Content-Type: application/json")
        @POST("/auth/login")
        Call<Auth> login(@Body JsonObject body);

        @GET("/auth/logout")
        Call<Logoff> logout();

        @GET("/users/me")
        Call<User> getUserData(@Header("Authorization") String authHeader);
    }

    public static void login(LoginCallback callback, String login, String password) {
        // Создаем простой REST адаптер, с помощью которого отправим запрос
        Retrofit retrofit =
            new Retrofit.Builder()
                .baseUrl(CLOUD_STORAGE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Создаем параметры запроса, которые будут отправлены как json
        JsonObject paramObject = new JsonObject();
        paramObject.addProperty("login", login);
        paramObject.addProperty("password", password);
        // Готовим метод к вызову
        Call<Auth> call = cloud_storage.login(paramObject);
        // Ждем ответ
        // В случае успеха приходит токен - надо его сохранить
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<Auth>() {
            @Override
            public void onResponse(Call<Auth> call, Response<Auth> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401) {
                        callback.authError(true); // token удалится
                        return;
                    }
                }
                else {
                    Auth auth = response.body();
                    if (auth != null)
                        if (auth.getToken() != null) {
                            // Здесь окажемся, если вход успешный
                            callback.onSuccess(auth.getToken());
                            return;
                        }
                    callback.authError(true); // token удалится
                }
                // Здесь программа окажется если вход не удался
                callback.authError(false); // token НЕ удалится
            }

            @Override
            public void onFailure(Call<Auth> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.networkError(t.toString()); // token НЕ удалится
            }
        });
    }

    public static void logout() {
        // Создаем простой REST адаптер, с помощью которого отправим запрос
        Retrofit retrofit =
            new Retrofit.Builder()
                .baseUrl(CLOUD_STORAGE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);

        // Готовим метод к вызову
        Call<Logoff> call = cloud_storage.logout();
        // Ждем ответ
        call.enqueue(new Callback<Logoff>() {
            @Override
            public void onResponse(Call<Logoff> call, Response<Logoff> response) {
            }

            @Override
            public void onFailure(Call<Logoff> call, Throwable t) {
                // Происходит в случае сетевых ошибок
            }
        });
    }

    private static void getUserData(@NonNull StorageCallback callback, UserDataReceiveCallback finish) {
        // Проверка, что пользователь залогинен
        String token = callback.getToken();
        if (token == null) {
            callback.onAuthError(true); // token удалится
            return;
        }

        // Создаем простой REST адаптер, с помощью которого отправим запрос
        Retrofit retrofit =
            new Retrofit.Builder()
                .baseUrl(CLOUD_STORAGE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Готовим метод к вызову
        Call<User> call = cloud_storage.getUserData("Bearer "+token);
        // Ждем ответ
        // В случае успеха приходят данные залогиненного пользователя - надо их сохранить
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 404)
                        callback.onLoadStorageFailure(); // token НЕ удалится
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                }
                else {
                    User user = response.body();
                    if (user != null)
                        if (user.getRootFolder() != null) {
                            // Здесь окажемся, если загрузка успешна
                            finish.onLoadFinished(user);
                            return;
                        }
                    // Здесь программа окажется если загрузка данных не удалась
                    callback.onLoadStorageFailure(); // token НЕ удалится
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private static void getFolders(StorageCallback callback, FoldersDataReceiveCallback finish) {
        Log.d("!!!5", "getFoldersAndFiles");
        finish.onLoadFinished(new ArrayList<>());
    }

    private interface UserDataReceiveCallback {
        void onLoadFinished(User user);
    }

    private interface FoldersDataReceiveCallback {
        void onLoadFinished(ArrayList<Folder> folders);
    }

    public static void getStorageData(@NonNull StorageCallback callback) {
        Storage model = new Storage();
        final User user = callback.getUser();

        if (user == null || user.getRootFolder() == null) {
            getUserData(callback, (u) -> {
                if (u == null) return;
                model.setUser(u);
                getFolders(callback, (f) -> {
                    if (f == null) return;
                    model.setFolders(f);
                    callback.onLoadStorageSuccess(model);
                });
            });
        }
        else {
            model.setUser(user);
            getFolders(callback, (f) -> {
                if (f == null) return;
                model.setFolders(f);
                callback.onLoadStorageSuccess(model);
            });
        }
        // нельзя, т.к. методы выше ассинхронные!!! callback.onStorageData(model);
    }
}
