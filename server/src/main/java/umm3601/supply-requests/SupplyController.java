package umm3601.supply.requests;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.Controller;

/**
 * Controller that manages requests for info about supplies.
 */
public class SupplyController implements Controller {

  private static final String API_USERS = "/api/requests";
  private static final String API_USER_BY_ID = "/api/requests/{id}";
  static final String SCHOOL_KEY = "school";
  static final String GRADE_KEY = "grade";
  static final String DESCRIPTION_KEY = "description";
  static final String PROPERTIES_KEY = "properties";
  static final Number QUANTITY_KEY = "quantity"

  private final JacksonMongoCollection<Supply> userCollection;

  /**
   * Construct a controller for supplies.
   *
   * @param database the database containing supply data
   */
  public SupplyController(MongoDatabase database) {
    supplyCollection = JacksonMongoCollection.builder().build(
        database,
        "supplies",
        Supply.class,
        UuidRepresentation.STANDARD);
  }

  /**
   * Set the JSON body of the response to be the single supply
   * specified by the `id` parameter in the request
   *
   * @param ctx a Javalin HTTP context
   */
  public void getUser(Context ctx) {
    String id = ctx.pathParam("id");
    Supply supply;

    try {
      supply = supplyCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested supply id wasn't a legal Mongo Object ID.");
    }
    if (supply == null) {
      throw new NotFoundResponse("The requested supply was not found");
    } else {
      ctx.json(supply);
      ctx.status(HttpStatus.OK);
    }
  }

  /**
   * Set the JSON body of the response to be a list of all the supplies returned from the database
   * that match any requested filters and ordering
   *
   * @param ctx a Javalin HTTP context
   */
  public void getUsers(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);

    // All three of the find, sort, and into steps happen "in parallel" inside the
    // database system. So MongoDB is going to find the supplies with the specified
    // properties, return those sorted in the specified manner, and put the
    // results into an initially empty ArrayList.
    ArrayList<User> matchingUsers = userCollection
      .find(combinedFilter)
      .sort(sortingOrder)
      .into(new ArrayList<>());

    // Set the JSON body of the response to be the list of supplies returned by the database.
    // According to the Javalin documentation (https://javalin.io/documentation#context),
    // this calls result(jsonString), and also sets content type to json
    ctx.json(matchingUsers);

    // Explicitly set the context status to OK
    ctx.status(HttpStatus.OK);
  }

  /**
   * Construct a Bson filter document to use in the `find` method based on the
   * query parameters from the context.
   *
   * This checks for the presence of the `quantity`, `school`, and `role` query
   * parameters and constructs a filter document that will match supplies with
   * the specified values for those fields.
   *
   * @param ctx a Javalin HTTP context, which contains the query parameters
   *    used to construct the filter
   * @return a Bson filter document that can be used in the `find` method
   *   to filter the database collection of supplies
   */
  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with an empty list of filters

    if (ctx.queryParamMap().containsKey(QUANTITY_KEY)) {
      int targeQuant = ctx.queryParamAsClass(QUANTITY_KEY, Integer.class)
        .check(it -> it > 0, "Item is not requested " + ctx.queryParam(QUANTITY_KEY))
        .check(it -> it < REASONABLE_AGE_LIMIT,
          "User's quantity must be less than " + REASONABLE_AGE_LIMIT + "; you provided " + ctx.queryParam(AGE_KEY))
        .get();
      filters.add(eq(QUANTITY_KEY, targetQuant));
    }
    if (ctx.queryParamMap().containsKey(COMPANY_KEY)) {
      Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(COMPANY_KEY)), Pattern.CASE_INSENSITIVE);
      filters.add(regex(COMPANY_KEY, pattern));
    }
    if (ctx.queryParamMap().containsKey(ROLE_KEY)) {
      String role = ctx.queryParamAsClass(ROLE_KEY, String.class)
        .check(it -> it.matches(ROLE_REGEX), "User must have a legal supply role")
        .get();
      filters.add(eq(ROLE_KEY, role));
    }

    // Combine the list of filters into a single filtering document.
    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  /**
   * Construct a Bson sorting document to use in the `sort` method based on the
   * query parameters from the context.
   *
   * This checks for the presence of the `sortby` and `sortorder` query
   * parameters and constructs a sorting document that will sort supplies by
   * the specified field in the specified order. If the `sortby` query
   * parameter is not present, it defaults to "description". If the `sortorder`
   * query parameter is not present, it defaults to "asc".
   *
   * @param ctx a Javalin HTTP context, which contains the query parameters
   *   used to construct the sorting order
   * @return a Bson sorting document that can be used in the `sort` method
   *  to sort the database collection of supplies
   */
  private Bson constructSortingOrder(Context ctx) {
    // Sort the results. Use the `sortby` query param (default "description")
    // as the field to sort by, and the query param `sortorder` (default
    // "asc") to specify the sort order.
    String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "description");
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortorder"), "asc");
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);
    return sortingOrder;
  }

  /**
   * Set the JSON body of the response to be a list of all the supply names and IDs
   * returned from the database, grouped by school
   *
   * This "returns" a list of supply names and IDs, grouped by school in the JSON
   * body of the response. The supply names and IDs are stored in `UserIdName` objects,
   * and the school description, the number of supplies in that school, and the list of supply
   * names and IDs are stored in `UserByCompany` objects.
   *
   * @param ctx a Javalin HTTP context that provides the query parameters
   *   used to sort the results. We support either sorting by school description
   *   (in either `asc` or `desc` order) or by the number of supplies in the
   *   school (`count`, also in either `asc` or `desc` order).
   */
  public void getUsersGroupedByCompany(Context ctx) {
    // We'll support sorting the results either by school description (in either `asc` or `desc` order)
    // or by the number of supplies in the school (`count`, also in either `asc` or `desc` order).
    String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortBy"), "_id");
    if (sortBy.equals("school")) {
      sortBy = "_id";
    }
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortOrder"), "asc");
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);

    // The `UserByCompany` class is a simple class that has fields for the school
    // description, the number of supplies in that school, and a list of supply names and IDs
    // (using the `UserIdName` class to store the supply names and IDs).
    // We're going to use the aggregation pipeline to group supplies by school, and
    // then count the number of supplies in each school. We'll also collect the supply
    // names and IDs for each supply in each school. We'll then convert the results
    // of the aggregation pipeline to `UserByCompany` objects.

    ArrayList<UserByCompany> matchingUsers = userCollection
      // The following aggregation pipeline groups supplies by school, and
      // then counts the number of supplies in each school. It also collects
      // the supply names and IDs for each supply in each school.
      .aggregate(
        List.of(
          // Project the fields we want to use in the next step, i.e., the _id, description, and school fields
          new Document("$project", new Document("_id", 1).append("description", 1).append("school", 1)),
          // Group the supplies by school, and count the number of supplies in each school
          new Document("$group", new Document("_id", "$school")
            // Count the number of supplies in each school
            .append("count", new Document("$sum", 1))
            // Collect the supply names and IDs for each supply in each school
            .append("supplies", new Document("$push", new Document("_id", "$_id").append("description", "$description")))),
          // Sort the results. Use the `sortby` query param (default "school")
          // as the field to sort by, and the query param `sortorder` (default
          // "asc") to specify the sort order.
          new Document("$sort", sortingOrder)
        ),
        // Convert the results of the aggregation pipeline to UserGroupResult objects
        // (i.e., a list of UserGroupResult objects). It is necessary to have a Java type
        // to convert the results to, and the JacksonMongoCollection will do this for us.
        UserByCompany.class
      )
      .into(new ArrayList<>());

    ctx.json(matchingUsers);
    ctx.status(HttpStatus.OK);
  }

  /**
   * Add a new supply using information from the context
   * (as long as the information gives "legal" values to User fields)
   *
   * @param ctx a Javalin HTTP context that provides the supply info
   *  in the JSON body of the request
   */
  public void addNewUser(Context ctx) {
    /*
     * The follow chain of statements uses the Javalin validator system
     * to verify that instance of `User` provided in this context is
     * a "legal" supply. It checks the following things (in order):
     *    - The supply has a value for the description (`usr.description != null`)
     *    - The supply description is not blank (`usr.description.length > 0`)
     *    - The provided email is valid (matches EMAIL_REGEX)
     *    - The provided quantity is > 0
     *    - The provided quantity is < REASONABLE_AGE_LIMIT
     *    - The provided role is valid (one of "admin", "editor", or "viewer")
     *    - A non-blank school is provided
     * If any of these checks fail, the Javalin system will throw a
     * `BadRequestResponse` with an appropriate error message.
     */
    String body = ctx.body();
    User newUser = ctx.bodyValidator(User.class)
      .check(usr -> usr.description != null && usr.description.length() > 0,
        "User must have a non-empty supply description; body was " + body)
      .check(usr -> usr.email.matches(EMAIL_REGEX),
        "User must have a legal email; body was " + body)
      .check(usr -> usr.quantity > 0,
        "User's quantity must be greater than zero; body was " + body)
      .check(usr -> usr.quantity < REASONABLE_AGE_LIMIT,
        "User's quantity must be less than " + REASONABLE_AGE_LIMIT + "; body was " + body)
      .check(usr -> usr.role.matches(ROLE_REGEX),
        "User must have a legal supply role; body was " + body)
      .check(usr -> usr.school != null && usr.school.length() > 0,
        "User must have a non-empty school description; body was " + body)
      .get();

    // Generate a supply avatar (you won't need this part for todos)
    newUser.avatar = generateAvatar(newUser.email);

    // Add the new supply to the database
    userCollection.insertOne(newUser);

    // Set the JSON response to be the `_id` of the newly created supply.
    // This gives the client the opportunity to know the ID of the new supply,
    // which it can then use to perform further operations (e.g., a GET request
    // to get and display the details of the new supply).
    ctx.json(Map.of("id", newUser._id));
    // 201 (`HttpStatus.CREATED`) is the HTTP code for when we successfully
    // create a new resource (a supply in this case).
    // See, e.g., https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
    // for a description of the various response codes.
    ctx.status(HttpStatus.CREATED);
  }

  /**
   * Delete the supply specified by the `id` parameter in the request.
   *
   * @param ctx a Javalin HTTP context
   */
  public void deleteUser(Context ctx) {
    String id = ctx.pathParam("id");
    DeleteResult deleteResult = userCollection.deleteOne(eq("_id", new ObjectId(id)));
    // We should have deleted 1 or 0 supplies, depending on whether `id` is a valid supply ID.
    if (deleteResult.getDeletedCount() != 1) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw new NotFoundResponse(
        "Was unable to delete ID "
          + id
          + "; perhaps illegal ID or an ID for an item not in the system?");
    }
    ctx.status(HttpStatus.OK);
  }

  /**
   * Utility function to generate an URI that points
   * at a unique avatar image based on a supply's email.
   *
   * This uses the service provided by gravatar.com; there
   * are numerous other similar services that one could
   * use if one wished.
   *
   * YOU DON'T NEED TO USE THIS FUNCTION FOR THE TODOS.
   *
   * @param email the email to generate an avatar for
   * @return a URI pointing to an avatar image
   */
  String generateAvatar(String email) {
    String avatar;
    try {
      // generate unique md5 code for identicon
      avatar = "https://gravatar.com/avatar/" + md5(email) + "?d=identicon";
    } catch (NoSuchAlgorithmException ignored) {
      // set to mystery person
      avatar = "https://gravatar.com/avatar/?d=mp";
    }
    return avatar;
  }

  /**
   * Utility function to generate the md5 hash for a given string
   *
   * @param str the string to generate a md5 for
   */
  public String md5(String str) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashInBytes = md.digest(str.toLowerCase().getBytes(StandardCharsets.UTF_8));

    StringBuilder result = new StringBuilder();
    for (byte b : hashInBytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Sets up routes for the `supply` collection endpoints.
   * A UserController instance handles the supply endpoints,
   * and the addRoutes method adds the routes to this controller.
   *
   * These endpoints are:
   *   - `GET /api/supplies/:id`
   *       - Get the specified supply
   *   - `GET /api/supplies?quantity=NUMBER&school=STRING&description=STRING`
   *      - List supplies, filtered using query parameters
   *      - `quantity`, `school`, and `description` are optional query parameters
   *   - `GET /api/supplysByCompany`
   *     - Get supply names and IDs, possibly filtered, grouped by school
   *   - `DELETE /api/supplies/:id`
   *      - Delete the specified supply
   *   - `POST /api/supplies`
   *      - Create a new supply
   *      - The supply info is in the JSON body of the HTTP request
   *
   * GROUPS SHOULD CREATE THEIR OWN CONTROLLERS THAT IMPLEMENT THE
   * `Controller` INTERFACE FOR WHATEVER DATA THEY'RE WORKING WITH.
   * You'll then implement the `addRoutes` method for that controller,
   * which will set up the routes for that data. The `Server#setupRoutes`
   * method will then call `addRoutes` for each controller, which will
   * add the routes for that controller's data.
   *
   * @param server The Javalin server instance
   */
  @Override
  public void addRoutes(Javalin server) {
    // Get the specified supply
    server.get(API_USER_BY_ID, this::getUser);

    // List supplies, filtered using query parameters
    server.get(API_USERS, this::getUsers);

    // Get the supplies, possibly filtered, grouped by school
    server.get("/api/SuppliesByCompany", this::getSuppliesGroupedBySchool);

    // Add new supply with the supply info being in the JSON body
    // of the HTTP request
    server.post(API_USERS, this::addNewUser);

    // Delete the specified supply
    server.delete(API_USER_BY_ID, this::deleteUser);
  }
}
