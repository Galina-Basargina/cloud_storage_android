package ru.cloudstorage.client.db;

import android.content.Context;

import ru.cloudstorage.client.MainActivity;

public class DatabasePreferences {
    Context context;
    private String login;
    private String token;

    public void init(Context context) {
        this.context = context;
        DatabaseHelper db = new DatabaseHelper(this.context);
        this.login = db.getSettings("login", null);
        this.token = db.getSettings("token", null);
        db.close();
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
        DatabaseHelper db = new DatabaseHelper(this.context);
        db.setSettings("login", this.login);
        db.close();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        DatabaseHelper db = new DatabaseHelper(this.context);
        db.setSettings("token", this.token);
        db.close();
    }

    // https://en.wikipedia.org/wiki/Singleton_pattern#Lazy_initialization
    private static volatile DatabasePreferences instance = null;
    private DatabasePreferences() {}
    public static DatabasePreferences getInstance() {
        if (instance == null) {
            synchronized (DatabasePreferences.class) {
                if (instance == null)
                    instance = new DatabasePreferences();
            }
        }
        return instance;
    }
}
