package ru.cloudstorage.client.rest;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import ru.cloudstorage.client.rest.cloudstorage.Auth;
import ru.cloudstorage.client.rest.cloudstorage.Empty;
import ru.cloudstorage.client.rest.cloudstorage.File;
import ru.cloudstorage.client.rest.cloudstorage.Folder;
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
        Call<Empty> logout();

        @GET("/users/me")
        Call<User> getUserData(@Header("Authorization") String authHeader);

        @GET("/folders/{id}")
        Call<Folder> getFolderData(@Path("id") int id, @Header("Authorization") String authHeader);

        @GET("/folders")
        Call<GetFolders> getFoldersData(@Header("Authorization") String authHeader);

        @GET("/files")
        Call<GetFiles> getFilesData(@Header("Authorization") String authHeader);

        @DELETE("/files/{id}")
        Call<Empty> removeFile(@Path("id") int id, @Header("Authorization") String authHeader);

        @DELETE("/folders/{id}")
        Call<Empty> removeFolder(@Path("id") int id, @Header("Authorization") String authHeader);

        @Headers("Content-Type: application/json")
        @PATCH("/files/{id}")
        Call<Empty> patchFile(@Path("id") int id, @Header("Authorization") String authHeader, @Body JsonObject body);

        @Headers("Content-Type: application/json")
        @PATCH("/folders/{id}")
        Call<Empty> patchFolder(@Path("id") int id, @Header("Authorization") String authHeader, @Body JsonObject body);
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
        Call<Empty> call = cloud_storage.logout();
        // Ждем ответ
        call.enqueue(new Callback<Empty>() {
            @Override
            public void onResponse(Call<Empty> call, Response<Empty> response) {
            }

            @Override
            public void onFailure(Call<Empty> call, Throwable t) {
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

    private static void removeFile(StorageCallback callback, String token, int fileId, RemoveFileReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Готовим метод к вызову
        Call<Empty> call = cloud_storage.removeFile(fileId, "Bearer "+token);
        // Ждем ответ
        // В случае успеха приходит ответ 200 (или 404)
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<Empty>() {
            @Override
            public void onResponse(Call<Empty> call, Response<Empty> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 404) {
                        finish.onLoadFinished(404); // работает не как ошибка - Not Found
                        return;
                    }
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                } else {
                    finish.onLoadFinished(200); // OK
                    return;
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<Empty> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private static void removeFolder(StorageCallback callback, String token, int folderId, RemoveFolderReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Готовим метод к вызову
        Call<Empty> call = cloud_storage.removeFolder(folderId, "Bearer "+token);
        // Ждем ответ
        // В случае успеха приходит ответ 200 (или 404)
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<Empty>() {
            @Override
            public void onResponse(Call<Empty> call, Response<Empty> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 404) {
                        finish.onLoadFinished(404); // работает не как ошибка - Not Found
                        return;
                    }
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                } else {
                    finish.onLoadFinished(200); // OK
                    return;
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<Empty> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private static void patchFile(
            StorageCallback callback,
            String token,
            int fileId,
            @Nullable Integer newFolder,
            @Nullable String newName,
            PatchFileReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Создаем параметры запроса, которые будут отправлены как json
        JsonObject paramObject = new JsonObject();
        if (newFolder != null)
            paramObject.addProperty("folder", newFolder);
        if (newName != null && !newName.isEmpty())
            paramObject.addProperty("original_filename", newName);
        // Готовим метод к вызову
        Call<Empty> call = cloud_storage.patchFile(fileId, "Bearer "+token, paramObject);
        // Ждем ответ
        // В случае успеха приходит ответ 200 (или 404)
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<Empty>() {
            @Override
            public void onResponse(Call<Empty> call, Response<Empty> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 404) {
                        finish.onLoadFinished(404); // работает не как ошибка - Not Found
                        return;
                    }
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                } else {
                    finish.onLoadFinished(200); // OK
                    return;
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<Empty> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                callback.onNetworkError(t.toString()); // token НЕ удалится
                finish.onLoadFinished(null);
            }
        });
    }

    private static void patchFolder(
            StorageCallback callback,
            String token,
            int fileId,
            @Nullable Integer newParent,
            @Nullable String newName,
            PatchFolderReceiveCallback finish) {
        // Создаем экземпляр интерфейса работы с сервером
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);
        // Создаем параметры запроса, которые будут отправлены как json
        JsonObject paramObject = new JsonObject();
        if (newParent != null)
            paramObject.addProperty("parent", newParent);
        if (newName != null && !newName.isEmpty())
            paramObject.addProperty("name", newName);
        // Готовим метод к вызову
        Call<Empty> call = cloud_storage.patchFolder(fileId, "Bearer "+token, paramObject);
        // Ждем ответ
        // В случае успеха приходит ответ 200 (или 404)
        // В случае провала - надо удалить сохраненный токен
        call.enqueue(new Callback<Empty>() {
            @Override
            public void onResponse(Call<Empty> call, Response<Empty> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 401)
                        callback.onAuthError(true); // token удалится
                    else if (response.code() == 404) {
                        finish.onLoadFinished(404); // работает не как ошибка - Not Found
                        return;
                    }
                    else
                        callback.onLoadStorageFailure(); // все остальные случаи
                } else {
                    finish.onLoadFinished(200); // OK
                    return;
                }
                finish.onLoadFinished(null);
            }

            @Override
            public void onFailure(Call<Empty> call, Throwable t) {
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
    private interface RemoveFileReceiveCallback {
        void onLoadFinished(Integer httpCode);
    }
    private interface RemoveFolderReceiveCallback {
        void onLoadFinished(Integer httpCode);
    }
    private interface PatchFileReceiveCallback {
        void onLoadFinished(Integer httpCode);
    }
    private interface PatchFolderReceiveCallback {
        void onLoadFinished(Integer httpCode);
    }

    public void getStorageData(@NonNull StorageCallback callback) {
        // Проверка, что пользователь залогинен (в андроид приложении
        // сохранен токен)
        // Если не залогинен, то будет выход с ошибкой аутентификации
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

    public void removeFileAndGetStorageData(@NonNull StorageCallback callback, File file) {
        // Проверка, что пользователь залогинен (в андроид приложении
        // сохранен токен)
        // Если не залогинен, то будет выход с ошибкой аутентификации
        final String token = callback.getToken();
        if (token == null) {
            callback.onAuthError(true); // token удалится
            return;
        }

        // Удаляем с сервера файл и обновляем модель данных
        removeFile(callback, token, file.getId().intValue(), (httpCode) -> {
            if (httpCode == null)
                ;
            else if (httpCode == 404) {
                // TODO: удалить объект из модели (а так же из StorageItems - это кэш названий)
                // Удаление файла на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
            else if (httpCode == 200) {
                // Удаление файла на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
        });
    }

    public void removeFolderAndGetStorageData(@NonNull StorageCallback callback, Folder folder) {
        // Проверка, что пользователь залогинен (в андроид приложении
        // сохранен токен)
        // Если не залогинен, то будет выход с ошибкой аутентификации
        final String token = callback.getToken();
        if (token == null) {
            callback.onAuthError(true); // token удалится
            return;
        }

        // Удаляем с сервера папку и обновляем модель данных
        removeFolder(callback, token, folder.getId().intValue(), (httpCode) -> {
            if (httpCode == null)
                ;
            else if (httpCode == 404) {
                // TODO: удалить объект (и все вложенные папки и файлы) из модели
                //       (а так же из StorageItems - это кэш названий)
                // Удаление папки на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
            else if (httpCode == 200) {
                // Удаление папки на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
        });
    }

    public void patchFileAndGetStorageData(
            @NonNull StorageCallback callback,
            @NonNull File file,
            @Nullable Integer newFolder,
            @Nullable String newName) {
        // Проверка, что пользователь залогинен (в андроид приложении
        // сохранен токен)
        // Если не залогинен, то будет выход с ошибкой аутентификации
        final String token = callback.getToken();
        if (token == null) {
            callback.onAuthError(true); // token удалится
            return;
        }

        // Переименовываем/перемещаем файл на сервере и обновляем модель данных
        patchFile(callback, token, file.getId().intValue(), newFolder, newName, (httpCode) -> {
            if (httpCode == null)
                ;
            else if (httpCode == 404) {
                // TODO: удалить объект из модели (а так же из StorageItems - это кэш названий)
                // Переименовывание/перемещение файла на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
            else if (httpCode == 200) {
                // Переименовывание/перемещение файла на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
        });
    }

    public void patchFolderAndGetStorageData(
            @NonNull StorageCallback callback,
            @NonNull Folder folder,
            @Nullable Integer newParent,
            @Nullable String newName) {
        // Проверка, что пользователь залогинен (в андроид приложении
        // сохранен токен)
        // Если не залогинен, то будет выход с ошибкой аутентификации
        final String token = callback.getToken();
        if (token == null) {
            callback.onAuthError(true); // token удалится
            return;
        }

        // Переименовываем/перемещаем папку на сервере и обновляем модель данных
        patchFolder(callback, token, folder.getId().intValue(), newParent, newName, (httpCode) -> {
            if (httpCode == null)
                ;
            else if (httpCode == 404) {
                // TODO: удалить объект из модели (а так же из StorageItems - это кэш названий)
                // Переименовывание/перемещение папки на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
            else if (httpCode == 200) {
                // Переименовывание/перемещение папки на сервере закончено успешно
                // конец эстафеты (дальше будет создаваться новая модель)
                getStorageData(callback);
            }
        });
    }
}
