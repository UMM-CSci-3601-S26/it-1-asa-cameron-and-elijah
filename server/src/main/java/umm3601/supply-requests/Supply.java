package umm3601.supply.requests;

import org.mongojack.Id;
import org.mongojack.ObjectId;

// There are two examples of suppressing CheckStyle
// warnings in this class. If you create new classes
// that mirror data in MongoDB and that will be managed
// by MongoJack, then you'll probably need to suppress
// the same warnings in your classes as well so that
// CheckStyle doesn't shout at you and cause the build
// to fail.

// Normally you'd want all fields to be private, but
// we need the fields in this class to be public since
// they will be written to by Mongo via the MongoJack
// library. We need to suppress the Visibility Modifier
// (https://checkstyle.sourceforge.io/config_design.html#VisibilityModifier)
// check in CheckStyle so that we don't get a failed
// build when Gradle runs CheckStyle.
@SuppressWarnings({"VisibilityModifier"})
public class Supply {

  @ObjectId @Id
  // By default Java field names shouldn't start with underscores.
  // Here, though, we *have* to use the name `_id` to match the
  // name of the field as used by MongoDB.
  @SuppressWarnings({"MemberName"})
  public String _id;

  public String description;
  public int quantity;
  public String school;
  public String grade;
  public String item;

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Supply)) {
      return false;
    }
    Supply other = (Supply) obj;
    return _id.equals(other._id);
  }

  @Override
  public int hashCode() {
    // This means that equal Supplies will hash the same, which is good.
    return _id.hashCode();
  }

  // Having some kind of `toString()` allows us to print `Supply`s,
  // which can be useful/necessary in error handling. This only
  // returns the name, but it could be extended to return more or
  // all of the fields combined into a single string.
  //
  // The other option would be to return `_id`, but that can be
  // `null` if we're trying to add a new `Supply` to the database
  // that doesn't yet have an `_id`, so returning `name` seemed
  // the better bet.
  @Override
  public String toString() {
    return description;
  }
}
