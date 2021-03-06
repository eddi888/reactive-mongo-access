package shopJava.queries;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.reactivestreams.Publisher;
import rx.Observable;
import shopJava.model.Credentials;
import shopJava.model.Order;
import shopJava.model.Result;
import shopJava.model.User;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static rx.RxReactiveStreams.toObservable;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ08RxStreamsWithObservables {

    public static void main(String[] args) throws Exception {
        new QueryJ08RxStreamsWithObservables();
    }

    private final DAO dao = new DAO();

    private class DAO {

        private final MongoCollection<Document> usersCollection;
        private final MongoCollection<Document> ordersCollection;

        DAO() {
            final MongoClient client = MongoClients.create(MONGODB_URI);
            final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
            this.usersCollection = db.getCollection(USERS_COLLECTION_NAME);
            this.ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        }

        Publisher<Document> findUserByName(final String name) {
            return usersCollection
                    .find(eq("_id", name))
                    .first();
        }

        Publisher<Document> findOrdersByUsername(final String username) {
            return ordersCollection
                    .find(eq("username", username));
        }
    }   // end DAO


    private Observable<String> logIn(final Credentials credentials) {
        return toObservable(dao.findUserByName(credentials.username))
                .map(doc -> new User(doc))      // no null check as we don't get null objects in the stream
                .toList()   // conversion to List to check whether we found a user with the specified name
                .map(users -> (Optional<User>)(users.size() == 0 ? Optional.empty() : Optional.of(users.get(0))))
                .map(optUser -> checkUserLoggedIn(optUser, credentials))
                .map(user -> user.name);
    }

    private Observable<Result> processOrdersOf(final String username) {
        return toObservable(dao.findOrdersByUsername(username))
                .map(doc -> new Order(doc))
                .toList()
                .map(orders -> new Result(username, orders));
    }

    private void eCommerceStatistics(final Credentials credentials) throws Exception {

        System.out.println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...");

        final CountDownLatch latch = new CountDownLatch(1);

        logIn(credentials)
                .flatMap(username -> processOrdersOf(username))
                .subscribe(
                        result -> result.display(),
                        t -> { System.err.println(t.toString()); latch.countDown(); },
                        () -> latch.countDown()
                );

        latch.await();
    }

    private QueryJ08RxStreamsWithObservables() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"));
    }
}
