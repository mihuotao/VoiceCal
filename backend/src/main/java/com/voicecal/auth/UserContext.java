package com.voicecal.auth;

public class UserContext {

    private static final ThreadLocal<LoginUser> holder = new ThreadLocal<>();

    public static void set(LoginUser user) {
        holder.set(user);
    }

    public static LoginUser get() {
        return holder.get();
    }

    public static Long getUserId() {
        LoginUser user = holder.get();
        return user != null ? user.getUserId() : null;
    }

    public static String getUsername() {
        LoginUser user = holder.get();
        return user != null ? user.getUsername() : null;
    }

    public static void clear() {
        holder.remove();
    }

}
