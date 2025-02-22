package ru.cloudstorage.client;

/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public final class SimpleService {
    public static final String API_URL = "https://api.github.com";

    public static class Contributor {
        public final String login;
        public final int contributions;

        public Contributor(String login, int contributions) {
            this.login = login;
            this.contributions = contributions;
        }
    }

    public interface GitHub {
        @GET("/repos/{owner}/{repo}/contributors")
        Call<List<Contributor>> contributors(@Path("owner") String owner, @Path("repo") String repo);
    }

    public static void foo() {
        // Create a very simple REST adapter which points the GitHub API.
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(API_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

        // Create an instance of our GitHub API interface.
        GitHub github = retrofit.create(GitHub.class);

        // Create a call instance for looking up Retrofit contributors.
        Call<List<Contributor>> call = github.contributors("square", "retrofit");

        call.enqueue(new Callback<List<Contributor>>() {
            @Override
            public void onResponse(Call<List<Contributor>> call, Response<List<Contributor>> response) {
                List<Contributor> contributors = response.body();
                for (Contributor contributor : contributors) {
                    Log.d("!!!" , contributor.login + " (" + contributor.contributions + ")");
                }
            }

            @Override
            public void onFailure(Call<List<Contributor>> call, Throwable t) {
                //Handle failure
                Log.d("!!!" , "onFailure");
            }
        });
    }

    public static final String CLOUD_STORAGE_URL = "http://336707.simplecloud.ru";

    public static class Auth {
        public final String token;
        public Auth(String token) {
            this.token = token;
        }
    }

    public interface CloudStorage {
        @Headers("Content-Type: application/json")
        @POST("/auth/login")
        Call<Auth> login(@Body JsonObject body);
    }

    public static void voo() {
        // Create a very simple REST adapter which points the GitHub API.
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(CLOUD_STORAGE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

        // Create an instance of our GitHub API interface.
        CloudStorage cloud_storage = retrofit.create(CloudStorage.class);

        // Create a call instance for looking up Retrofit contributors.
        JsonObject paramObject = new JsonObject();
        paramObject.addProperty("login", "galina");
        paramObject.addProperty("password", "??????");

        Call<Auth> call = cloud_storage.login(paramObject);

        call.enqueue(new Callback<Auth>() {
            @Override
            public void onResponse(Call<Auth> call, Response<Auth> response) {
                Auth auth = response.body();
                Log.d("!!!", response.toString());
                Log.d("!!!", auth.token);
            }

            @Override
            public void onFailure(Call<Auth> call, Throwable t) {
                //Handle failure
                Log.d("!!!", "onFailure Auth " + t.toString());
            }
        });
    }
}
