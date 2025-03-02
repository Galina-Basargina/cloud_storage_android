package ru.cloudstorage.client.rest;

import android.util.Log;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import ru.cloudstorage.client.db.DatabasePreferences;
import ru.cloudstorage.client.ui.login.LoginCallback;

public final class SimpleService {
    public static final String CLOUD_STORAGE_URL = "http://336707.simplecloud.ru";

    public static class Auth {
        private String token;
        public String getToken() {
            return this.token;
        }
        public void setToken(String token) {
            this.token = token;
        }
    }

    public interface CloudStorage {
        @Headers("Content-Type: application/json")
        @POST("/auth/login")
        Call<Auth> login(@Body JsonObject body);

        @GET("/auth/logout")
        void logout();
    }

    public static void login(LoginCallback errorCallback, String login, String password) {
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
                    if (response.code() == 401)
                        errorCallback.authError(true); // token удалится
                }
                else {
                    Auth auth = response.body();
                    if (auth != null)
                        if (auth.getToken() != null) {
                            // Здесь окажемся, если вход успешный
                            errorCallback.onSuccess(auth.getToken());
                            return;
                        }
                    errorCallback.authError(true); // token удалится
                }
                // Здесь программа окажется если вход не удался
                errorCallback.authError(false); // token НЕ удалится
            }

            @Override
            public void onFailure(Call<Auth> call, Throwable t) {
                // Происходит в случае сетевых ошибок
                Log.d("!!!", "onFailure Auth " + t.toString());
                errorCallback.networkError(t.toString()); // token НЕ удалится
            }
        });
    }
}
