import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Inventory } from './inventory';

/**
 * Service that provides the interfaclient/src/app/users/user-profile.component.html client/src/app/users/user-profile.component.scss client/src/app/users/user-profile.component.spec.ts client/src/app/users/user-profile.component.tsce for getting information
 * about `Users` from the server.
 */
@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  // The private `HttpClient` is *injected* into the service
  // by the Angular framework. This allows the system to create
  // only one `HttpClient` and share that across all services
  // that need it, and it allows us to inject a mock version
  // of `HttpClient` in the unit tests so they don't have to
  // make "real" HTTP calls to a server that might not exist or
  // might not be currently running.
  private httpClient = inject(HttpClient);

  // The URL for the users part of the server API.
  readonly inventoryUrl: string = `${environment.apiUrl}inventories`;

  private readonly ITEM_KEY = 'item';
  private readonly QUANTITY_KEY = 'quantity';
  private readonly  PROPERTIES_KEY = 'properties';

  /**
   * Get all the users from the server, filtered by the information
   * in the `filters` map.
   *
   * It would be more consistent with `UserListComponent` if this
   * only supported filtering on age and role, and left company to
   * just be in `filterUsers()` below. We've included it here, though,
   * to provide some additional examples.
   *
   * @param filters a map that allows us to specify a target role, age,
   *  or company to filter by, or any combination of those
   * @returns an `Observable` of an array of `Users`. Wrapping the array
   *  in an `Observable` means that other bits of of code can `subscribe` to
   *  the result (the `Observable`) and get the results that come back
   *  from the server after a possibly substantial delay (because we're
   *  contacting a remote server over the Internet).
   */
  getInventory(filters?: { item?: string; quantity?: number; properties?: string }): Observable<Inventory[]> {
    // `HttpParams` is essentially just a map used to hold key-value
    // pairs that are then encoded as "?key1=value1&key2=value2&…" in
    // the URL when we make the call to `.get()` below.
    let httpParams: HttpParams = new HttpParams();
    if (filters) {
      if (filters.item) {
        httpParams = httpParams.set(this.ITEM_KEY, filters.item);
      }
      if (filters.quantity) {
        httpParams = httpParams.set(this.QUANTITY_KEY, filters.quantity.toString());
      }
      if (filters.properties) {
        httpParams = httpParams.set(this.PROPERTIES_KEY, filters.properties);
      }
    }
    // Send the HTTP GET request with the given URL and parameters.
    // That will return the desired `Observable<User[]>`.
    return this.httpClient.get<Inventory[]>(this.inventoryUrl, {
      params: httpParams,
    });
  }

  /**
   * Get the `User` with the specified ID.
   *
   * @param id the ID of the desired user
   * @returns an `Observable` containing the resulting user.
   */
  getInventoryById(id: string): Observable<Inventory> {
    // The input to get could also be written as (this.userUrl + '/' + id)
    return this.httpClient.get<Inventory>(`${this.inventoryUrl}/${id}`);
  }
  getInventoryByDescription(item: string): Observable<Inventory> {
    // The input to get could also be written as (this.userUrl + '/' + id)
    return this.httpClient.get<Inventory>(`${this.inventoryUrl}/${item}`);
  }

  /**
   * A service method that filters an array of `User` using
   * the specified filters.
   *
   * Note that the filters here support partial matches. Since the
   * matching is done locally we can afford to repeatedly look for
   * partial matches instead of waiting until we have a full string
   * to match against.
   *
   * @param inventories the array of `Users` that we're filtering
   * @param filters the map of key-value pairs used for the filtering
   * @returns an array of `Users` matching the given filters
   */
  filterInventories(inventories: Inventory[], filters: { item?: string; properties?: string}): Inventory[] { // skipcq: JS-0105
    let filteredInventories = inventories;

    // Filter by name
    if (filters.item) {
      filters.item = filters.item.toLowerCase();
      filteredInventories = filteredInventories.filter(inventory => inventory.item.toLowerCase().indexOf(filters.item) !== -1);
    }
    if (filters.properties) {
      filters.properties = filters.properties.toLowerCase();
      filteredInventories = filteredInventories.filter(inventory => inventory.properties.toLowerCase().indexOf(filters.properties) !== -1);
    }

    return filteredInventories;
  }

  addInventory(newInventory: Partial<Inventory>): Observable<string> {
    // Send post request to add a new user with the user data as the body.
    // `res.id` should be the MongoDB ID of the newly added `User`.
    return this.httpClient.post<{item: string}>(this.inventoryUrl, newInventory).pipe(map(response => response.item));
  }
}
