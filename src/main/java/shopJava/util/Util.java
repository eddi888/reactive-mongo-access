package shopJava.util;

import shopJava.model.Credentials;
import shopJava.model.User;

import java.util.Optional;

public class Util {

    public static User checkUserLoggedIn(final Optional<User> optUser, final Credentials credentials) {

        if (!optUser.isPresent()) {     // replaces if (user != null)
            throw new RuntimeException(new IllegalAccessException("User unknown: " + credentials.username));
        }
        final User user = optUser.get();
        if (!user.name.equals(credentials.username)) {
            throw new RuntimeException(new IllegalAccessException("Incorrect username: " + credentials.username));
        }
        if (!user.password.equals(credentials.password)) {
            throw new RuntimeException(new IllegalAccessException("Bad password supplied for user: " + credentials.username));
        }
        return user;
    }

    public static void sleep(final int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
