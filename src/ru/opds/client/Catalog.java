package ru.opds.client;

public class Catalog {
    public String name;
    public String url;
    public String login;
    public String password;

    public Catalog(String name, String url) {
        this.name = name;
        this.url = url;
        this.login = "";
        this.password = "";
    }

    public Catalog(String name, String url, String login, String password) {
        this.name = name;
        this.url = url;
        this.login = login == null ? "" : login;
        this.password = password == null ? "" : password;
    }

    public boolean hasAuth() {
        return login != null && login.length() > 0;
    }
}