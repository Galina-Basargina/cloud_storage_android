package ru.cloudstorage.client.rest;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.OkHttpClient;
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
import retrofit2.http.Path;
import ru.cloudstorage.client.rest.cloudstorage.Auth;
import ru.cloudstorage.client.rest.cloudstorage.File;
import ru.cloudstorage.client.rest.cloudstorage.Folder;
import ru.cloudstorage.client.rest.cloudstorage.Logoff;
import ru.cloudstorage.client.rest.cloudstorage.Storage;
import ru.cloudstorage.client.rest.cloudstorage.User;
import ru.cloudstorage.client.ui.login.LoginCallback;
import ru.cloudstorage.client.ui.storage.StorageCallback;

public final class SimpleService {

    // https://en.wikipedia.org/wiki/Singleton_pattern#Lazy_initialization
    private static volatile SimpleService instance = null;
    private SimpleService() {}
    public static SimpleService getInstance() {
        if (instance == null) {
            synchronized (SimpleService.class) {
                if (instance == null) {
                    instance = new SimpleService();
                    instance.init();
                }
            }
        }
        return instance;
    }

    private static OkHttpClient okHttpClient;
    private static Retrofit retrofit;

    private void init() {
        okHttpClient = new OkHttpClient.Builder().build();
        // Создаем простой REST адаптер, с помощью которого будем отправлять запросы
        retrofit = new Retrofit.Builder()
            .baseUrl(CLOUD_STORAGE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }

    public void finish() {
        Log.d("!!!", "finished");
        // Отменить все запросы
        okHttpClient.dispatcher().cancelAll();
        // Закрыть соединения
        okHttpClient.connectionPool().evictAll();
        // Остановить исполнители
        okHttpClient.dispatcher().executorService().shutdown();
    }
    private static final String CLOUD_STORAGE_URL = "http://336707.simplecloud.ru";

    private static class GetFolders {
        List<Folder> folders;
    }
    private static class GetFiles {
        List<File> files;
    }
    private interface CloudStorage {
        @Headers("Content-Type: application/json")
        @POST("/auth/login")
        Call<Auth> login(@Body JsonObject body);

        @GET("/auth/logout")
        Call<Logoff> logout();

        @GET("/users/me")
        Call<User> getUserData(@Header("Authorization") String authHeader);

        @GET("/folders/{id}")
        Call<Folder> getFolderData(@Path("id") int id, @Header("Authorization") String authHeader);

        @GET("/folders")
        Call<GetFolders> getFoldersData(@Header("Authorization") String authHeader);

        @GET("/files")
        Call<GetFiles> getFilesData(@Header("Authorization") String authHeader);
    }

    public void login(LoginCallback callback, String login, String password) {
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

    public void logout() {
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

    private static void getUserData(@NonNull StorageCallback callback, String token, UserDataReceiveCallback finish) {
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

    private static void getFolder(StorageCallback callback, String token, int folderId, FolderDataReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Готовим метод к вызову
        Call<Folder> call = cloud_storage.getFolderData(folderId, "Bearer "+token);
        // Ждем ответ
        // В случае успеха приходят данные залогиненного пользователя - надо их сохранить
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<Folder>() {
            @Override
            public void onResponse(Call<Folder> call, Response<Folder> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 404)
                        callback.onLoadStorageFailure(); // token НЕ удалится
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                }
                else {
                    Folder folder = response.body();
                    if (folder != null)
                        if (folder.getOwner() != null) {
                            // Здесь окажемся, если загрузка успешна
                            folder.setId(folderId);
                            finish.onLoadFinished(folder);
                            return;
                        }
                    // Здесь программа окажется если загрузка данных не удалась
                    callback.onLoadStorageFailure(); // token НЕ удалится
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<Folder> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private static void getFolders(StorageCallback callback, String token, FoldersDataReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Готовим метод к вызову
        Call<GetFolders> call = cloud_storage.getFoldersData("Bearer "+token);
        // Ждем ответ
        // В случае успеха приходят данные залогиненного пользователя - надо их сохранить
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<GetFolders>() {
            @Override
            public void onResponse(Call<GetFolders> call, Response<GetFolders> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 405)
                        callback.onLoadStorageFailure(); // token НЕ удалится
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                }
                else {
                    GetFolders folders = response.body();
                    if (folders != null && folders.folders != null)
                        if (!folders.folders.isEmpty()) {
                            // Здесь окажемся, если загрузка успешна
                            finish.onLoadFinished(folders.folders);
                            return;
                        }
                    // Здесь программа окажется если загрузка данных не удалась
                    callback.onLoadStorageFailure(); // token НЕ удалится
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<GetFolders> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private static void getFiles(StorageCallback callback, String token, FilesDataReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Готовим метод к вызову
        Call<GetFiles> call = cloud_storage.getFilesData("Bearer "+token);
        // Ждем ответ
        // В случае успеха приходят данные залогиненного пользователя - надо их сохранить
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<GetFiles>() {
            @Override
            public void onResponse(Call<GetFiles> call, Response<GetFiles> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                } else {
                    GetFiles files = response.body();
                    if (files != null && files.files != null)
                        if (!files.files.isEmpty()) {
                            // Здесь окажемся, если загрузка успешна
                            finish.onLoadFinished(files.files);
                            return;
                        }
                    // Здесь программа окажется если загрузка данных не удалась
                    callback.onLoadStorageFailure(); // token НЕ удалится
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<GetFiles> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private interface UserDataReceiveCallback {
        void onLoadFinished(User user);
    }

    private interface FolderDataReceiveCallback {
        void onLoadFinished(Folder folder);
    }

    private interface FoldersDataReceiveCallback {
        void onLoadFinished(List<Folder> folders);
    }

    private interface FilesDataReceiveCallback {
        void onLoadFinished(List<File> files);
    }

    public void getStorageData(@NonNull StorageCallback callback) {
        // Проверка, что пользователь залогинен
        // если не залогинен, то будет выход с ошибкой аутентификации
        final String token = callback.getToken();
        if (token == null) {
            callback.onAuthError(true); // token удалится
            return;
        }

        // Создаем НОВУЮ МОДЕЛЬ ДАННЫХ и получаем данные о пользователе
        // пользователя может и не быть, если это первая загрузка
        // Если загрузка НОВОЙ модели завершится успешно, то СТАРАЯ
        // модель будет ЗАМЕНЕНА
        Storage model = new Storage();
        final User user = callback.getUser();

        if (user == null || user.getRootFolder() == null) {
            // Первая загрузка без данных о пользователе и его корневой
            // папке
            getUserData(callback, token, (u) -> {
                if (u == null) return;
                // Удалось получить с сервера данные о пользователе
                model.setUser(u);
                // Сразу получаем с сервера данные о корневой папке, чтобы
                // потом её не искать
                getFolder(callback, token, u.getRootFolder(), (f) -> {
                    if (f == null) return;
                    // Получаем с сервера список папок и выбираем корневую
                    // папку как текущую
                    getFolders(callback, token, (fols) -> {
                        if (fols == null) return;
                        for (Folder tmp : fols)
                            if (tmp.getId().intValue() == f.getId().intValue()) {
                                model.setCurrentFolder(tmp);
                                break;
                            }
                        // Сохраняем список папок в модель
                        model.setFolders(fols);
                        // Получаем с сервера список файлов и сохраняем его
                        // в модель
                        getFiles(callback, token, (fils) -> {
                            if (fils == null) return;
                            // Сохраняем список файлов в модель
                            model.setFiles(fils);
                            // Изменение модели данных закончено успешно
                            // конец эстафеты (загрузки данных сервера)
                            callback.onLoadStorageSuccess(model);
                        });
                    });
                });
            });
        }
        else {
            // Берём из старой модели данных информацию о текущей папке
            // и запоминаем (потом потребуется её id)
            final Folder previousFolder = callback.getCurrentFolder();
            // Сохраняем в новую модель данные о пользователе из старой
            // (данные о пользователе больше не загружаются, они уже есть)
            model.setUser(user);
            // Получаем с сервера список папок и используем данные о текущей
            // папке из старой модели и выбираем ее как текущую
            getFolders(callback, token, (fols) -> {
                if (fols == null) return;
                // Стелим себе простынку, ведь если информация о текущей папке
                // из старой модели будет удалена с сервера, то сохраняем
                // информацию о корневой папке для того, чтобы сделать её текущей
                Folder rf = null;
                // В новой модели сущности (объекты) папок и файлов будут новыми,
                // поэтому надо восстановить в новой модели данные (указатель) на
                // текущую папку (указатель на объект)
                for (Folder tmp : fols) {
                    if (tmp.getId().intValue() == previousFolder.getId().intValue()) {
                        model.setCurrentFolder(tmp);
                        break;
                    }
                    if (tmp.getId().intValue() == user.getRootFolder().intValue())
                        rf = tmp;
                }
                // Проверяем, удалена ли была информация о текущей папке с сервера?
                // если да, то сохраняем указатель корневой папки как текущую
                if (model.getCurrentFolder() == null)
                    model.setCurrentFolder(rf);
                // Сохраняем список файлов в модель
                model.setFolders(fols);
                // Получаем с сервера список файлов и сохраняем его в модель
                getFiles(callback, token, (fils) -> {
                    if (fils == null) return;
                    // Сохраняем список файлов в модель
                    model.setFiles(fils);
                    // Изменение модели данных закончено успешно
                    // конец эстафеты (загрузки данных сервера)
                    callback.onLoadStorageSuccess(model);
                });
            });
        }
        // нельзя, т.к. методы выше ассинхронные!!! callback.onStorageData(model);
    }

    public void removeFileAndGetStorageData(@NonNull StorageCallback callback) {
        getStorageData(callback);
    }

    public void removeFolderAndGetStorageData(@NonNull StorageCallback callback) {
        getStorageData(callback);
    }
}
