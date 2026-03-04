package umm3601.supply_requests;

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

  private static final String API_SUPPLY_REQUESTS = "/api/supply_requests";
  private static final String API_SUPPLY_REQUESTS_BY_ID = "/api/supply_requests/{id}";
  static final String GRADE_KEY = "grade";
  static final String SCHOOL_KEY = "school";
  static final String ITEM_KEY = "item";
  static final String DESCRIPTION_KEY = "description";
  static final String PROPERTIES_KEY = "properties";
  static final String QUANTITY_KEY = "quantity";



  private final JacksonMongoCollection<Supply> supplyCollection;

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
  public void getSupply(Context ctx) {
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
  public void getSupplies(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);

    // All three of the find, sort, and into steps happen "in parallel" inside the
    // database system. So MongoDB is going to find the supplies with the specified
    // properties, return those sorted in the specified manner, and put the
    // results into an initially empty ArrayList.
    ArrayList<Supply> matchingSupplies = supplyCollection
      .find(combinedFilter)
      .sort(sortingOrder)
      .into(new ArrayList<>());

    // Set the JSON body of the response to be the list of supplies returned by the database.
    // According to the Javalin documentation (https://javalin.io/documentation#context),
    // this calls result(jsonString), and also sets content type to json
    ctx.json(matchingSupplies);

    // Explicitly set the context status to OK
    ctx.status(HttpStatus.OK);
  }

  /**
   * Construct a Bson filter document to use in the `find` method based on the
   * query parameters from the context.
   *
   * This checks for the presence of the `age`, `company`, and `role` query
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

    if (ctx.queryParamMap().containsKey(SCHOOL_KEY)) {
      Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(SCHOOL_KEY)), Pattern.CASE_INSENSITIVE);
      filters.add(regex(SCHOOL_KEY, pattern));
    }

     if (ctx.queryParamMap().containsKey(ITEM_KEY)) {
      Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(ITEM_KEY)), Pattern.CASE_INSENSITIVE);
      filters.add(regex(ITEM_KEY, pattern));
    }

    if (ctx.queryParamMap().containsKey(GRADE_KEY)) {
      Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(GRADE_KEY)), Pattern.CASE_INSENSITIVE);
      filters.add(regex(GRADE_KEY, pattern));
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
   * Set the JSON body of the response to be a list of all the supply descriptions and IDs
   * returned from the database, grouped by company
   *
   * This "returns" a list of supply descriptions and IDs, grouped by company in the JSON
   * body of the response. The supply descriptions and IDs are stored in `SupplyIdDescription` objects,
   * and the company description, the number of supplies in that company, and the list of supply
   * descriptions and IDs are stored in `SupplyByCompany` objects.
   *
   * @param ctx a Javalin HTTP context that provides the query parameters
   *   used to sort the results. We support either sorting by company description
   *   (in either `asc` or `desc` order) or by the number of supplies in the
   *   company (`count`, also in either `asc` or `desc` order).
   */
  public void getSuppliesGroupedByCompany(Context ctx) {
    // We'll support sorting the results either by company description (in either `asc` or `desc` order)
    // or by the number of supplies in the company (`count`, also in either `asc` or `desc` order).
    String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortBy"), "_id");
    if (sortBy.equals("company")) {
      sortBy = "_id";
    }
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortOrder"), "asc");
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);
  }

    // The `SupplyByCompany` class is a simple class that has fields for the company
    // description, the number of supplies in that company, and a list of supply descriptions and IDs
    // (using the `SupplyIdDescription` class to store the supply descriptions and IDs).
    // We're going to use the aggregation pipeline to group supplies by company, and
    // then count the number of supplies in each company. We'll also collect the supply
    // descriptions and IDs for each supply in each company. We'll then convert the results
    // of the aggregation pipeline to `SupplyByCompany` objects.

  //   ArrayList<SupplyBySchool> matchingSupplies = supplyCollection
  //     // The following aggregation pipeline groups supplies by company, and
  //     // then counts the number of supplies in each company. It also collects
  //     // the supply descriptions and IDs for each supply in each company.
  //     .aggregate(
  //       List.of(
  //         // Project the fields we want to use in the next step, i.e., the _id, description, and company fields
  //         new Document("$project", new Document("_id", 1).append("description", 1).append("company", 1)),
  //         // Group the supplies by company, and count the number of supplies in each company
  //         new Document("$group", new Document("_id", "$company")
  //           // Count the number of supplies in each company
  //           .append("count", new Document("$sum", 1))
  //           // Collect the supply descriptions and IDs for each supply in each company
  //           .append("supplies", new Document("$push", new Document("_id", "$_id").append("description", "$description")))),
  //         // Sort the results. Use the `sortby` query param (default "company")
  //         // as the field to sort by, and the query param `sortorder` (default
  //         // "asc") to specify the sort order.
  //         new Document("$sort", sortingOrder)
  //       ),
  //       // Convert the results of the aggregation pipeline to SupplyGroupResult objects
  //       // (i.e., a list of SupplyGroupResult objects). It is necessary to have a Java type
  //       // to convert the results to, and the JacksonMongoCollection will do this for us.
  //       SupplyByCompany.class
  //     )
  //     .into(new ArrayList<>());

  //   ctx.json(matchingSupplies);
  //   ctx.status(HttpStatus.OK);
  // }

  /**
   * Add a new supply using information from the context
   * (as long as the information gives "legal" values to Supply fields)
   *
   * @param ctx a Javalin HTTP context that provides the supply info
   *  in the JSON body of the request
  //  */
  // public void addNewSupply(Context ctx) {
    /*
     * The follow chain of statements uses the Javalin validator system
     * to verify that instance of `Supply` provided in this context is
     * a "legal" supply. It checks the following things (in order):
     *    - The supply has a value for the description (`usr.description != null`)
     *    - The supply description is not blank (`usr.description.length > 0`)
     *    - The provided email is valid (matches EMAIL_REGEX)
     *    - The provided quantity is > 0
     *    - The provided quantity is < REASONABLE_AGE_LIMIT
     *    - The provided role is valid (one of "admin", "editor", or "viewer")
     *    - A non-blank company is provided
     * If any of these checks fail, the Javalin system will throw a
     * `BadRequestResponse` with an appropriate error messquantity.
     */
    // String body = ctx.body();
    // Supply newSupply = ctx.bodyValidator(Supply.class)
    //   .check(usr -> usr.description != null && usr.description.length() > 0,
    //     "Supply must have a non-empty supply description; body was " + body)
    //   .check(usr -> usr.email.matches(EMAIL_REGEX),
    //     "Supply must have a legal email; body was " + body)
    //   .check(usr -> usr.quantity > 0,
    //     "Supply's quantity must be greater than zero; body was " + body)
    //   .check(usr -> usr.quantity < REASONABLE_AGE_LIMIT,
    //     "Supply's quantity must be less than " + REASONABLE_AGE_LIMIT + "; body was " + body)
    //   .check(usr -> usr.role.matches(ROLE_REGEX),
    //     "Supply must have a legal supply role; body was " + body)
    //   .check(usr -> usr.company != null && usr.company.length() > 0,
    //     "Supply must have a non-empty company description; body was " + body)
    //   .get();

    // Generate a supply avatar (you won't need this part for todos)

    // Add the new supply to the database
    // supplyCollection.insertOne(newSupply);

    // Set the JSON response to be the `_id` of the newly created supply.
    // This gives the client the opportunity to know the ID of the new supply,
    // which it can then use to perform further operations (e.g., a GET request
    // to get and display the details of the new supply).
    // ctx.json(Map.of("id", newSupply._id));
    // 201 (`HttpStatus.CREATED`) is the HTTP code for when we successfully
    // create a new resource (a supply in this case).
    // See, e.g., https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
    // for a description of the various response codes.
    // ctx.status(HttpStatus.CREATED);
  // }

  /**
   * Delete the supply specified by the `id` parameter in the request.
   *
   * @param ctx a Javalin HTTP context
   */
  public void deleteSupply(Context ctx) {
    String id = ctx.pathParam("id");
    DeleteResult deleteResult = supplyCollection.deleteOne(eq("_id", new ObjectId(id)));
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
   * at a unique avatar imquantity based on a supply's email.
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
   * A SupplyController instance handles the supply endpoints,
   * and the addRoutes method adds the routes to this controller.
   *
   * These endpoints are:
   *   - `GET /api/supplies/:id`
   *       - Get the specified supply
   *   - `GET /api/supplies?quantity=NUMBER&company=STRING&description=STRING`
   *      - List supplies, filtered using query parameters
   *      - `quantity`, `company`, and `description` are optional query parameters
   *   - `GET /api/suppliesByCompany`
   *     - Get supply descriptions and IDs, possibly filtered, grouped by company
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
    server.get(API_SUPPLY_REQUESTS_BY_ID, this::getSupply);

    // List supplies, filtered using query parameters
    server.get(API_SUPPLY_REQUESTS, this::getSupplies);

    // Get the supplies, possibly filtered, grouped by company
    // server.get("/api/suppliesByCompany", this::getSuppliesGroupedByCompany);

    // Add new supply with the supply info being in the JSON body
    // of the HTTP request
    // server.post(API_SUPPLIES, this::addNewSupply);

    // // Delete the specified supply
    // server.delete(API_USER_BY_ID, this::deleteSupply);
  }
}
