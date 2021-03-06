package shopJava.queries;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.Order;
import shopJava.model.User;
import shopJava.model.Credentials;
import shopJava.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ04bCompletionStageCompleteRefactored {

    private static final int nCores = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(nCores);

    public static void main(String[] args) throws Exception {
        new QueryJ04bCompletionStageCompleteRefactored();
    }

    private final DAO dao = new DAO();

    private class DAO {

        private final MongoCollection<Document> usersCollection;
        private final MongoCollection<Document> ordersCollection;

        DAO() {
            final MongoClient client = new MongoClient(new MongoClientURI(MONGODB_URI));
            final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
            this.usersCollection = db.getCollection(USERS_COLLECTION_NAME);
            this.ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        }

        private Optional<User> _findUserByName(final String name) {
            final Document doc = usersCollection
                    .find(eq("_id", name))
                    .first();
            return Optional.ofNullable(doc).map(User::new);
        }

        private List<Order> _findOrdersByUsername(final String username) {
            final List<Document> docs = ordersCollection
                    .find(eq("username", username))
                    .into(new ArrayList<>());
            return docs.stream()
                    .map(doc -> new Order(doc))
                    .collect(toList());
        }

        CompletionStage<Optional<User>> findUserByName(final String name) {
            Supplier<Optional<User>> supplier = () -> _findUserByName(name);
            return provideResultAsync(supplier, executor);
        }

        CompletionStage<List<Order>> findOrdersByUsername(final String username) {
            Supplier<List<Order>> supplier = () -> _findOrdersByUsername(username);
            return provideResultAsync(supplier, executor);
        }

        private <T> CompletionStage<T> provideResultAsync(final Supplier<T> supplier, final ExecutorService executorService) {

            final CompletableFuture<T> future = new CompletableFuture<>();

            executorService.execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            return future;
        }
    }   // end DAO


    private CompletionStage<String> logIn(final Credentials credentials) {
        return dao.findUserByName(credentials.username)
                .thenApply(optUser -> checkUserLoggedIn(optUser, credentials))
                .thenApply(user -> user.name);

    }

    private CompletionStage<Result> processOrdersOf(final String username) {
        return dao.findOrdersByUsername(username)
                .thenApply(orders -> new Result(username, orders));
    }

    private void eCommerceStatistics(final Credentials credentials, final boolean isLastInvocation) {

        System.out.println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...");

        logIn(credentials)
                .thenCompose(username -> processOrdersOf(username))     // flatMap of CompletionStage
                .whenComplete((result, t) -> {
                    if (t == null) {
                        result.display();
                    } else {
                        System.err.println(t.toString());
                    }
                    if (isLastInvocation) {
                        executor.shutdown();
                    }
                });
    }

    private QueryJ04bCompletionStageCompleteRefactored() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"), false);
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"), false);
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"), true);
    }
}
