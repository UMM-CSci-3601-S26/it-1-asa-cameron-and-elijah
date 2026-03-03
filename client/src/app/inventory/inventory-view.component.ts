import { Component, signal, inject, Signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { catchError, map, switchMap } from 'rxjs/operators';
import { InventoryCardComponent } from './inventory-card.component';
import { InventoryService } from './inventory.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { Inventory } from './inventory';

@Component({
  selector: 'app-inventory-request',
  templateUrl: './inventory-view.component.html',
  styleUrls: ['./inventory-view.component.scss'],
  imports: [InventoryCardComponent, MatCardModule],
})
export class InventoryProfileComponent {
  private route = inject(ActivatedRoute);
  private inventoryService = inject(InventoryService);

  user: Signal<Inventory> = toSignal(
    this.route.paramMap.pipe(
      // Map the paramMap into the id
      map((paramMap: ParamMap) => paramMap.get('id')),
      // Maps the `id` string into the Observable<User>,
      // which will emit zero or one values depending on whether there is a
      // `User` with that ID.
      switchMap((id: string) => this.inventoryService.getInventoryById(id)),
      catchError((_err) => {
        this.error.set({
          help: 'There was a problem loading the user – try again.',
          httpResponse: _err.message,
          message: _err.error?.title,
        });
        return of();
      })
      /*
       * You can uncomment the line that starts with `finalize` below to use that console message
       * as a way of verifying that this subscription is completing.
       * We removed it since we were not doing anything interesting on completion
       * and didn't want to clutter the console log
       */
      // finalize(() => console.log('We got a new user, and we are done!'))
    )
  );
  // The `error` will initially have empty strings for all its components.
  error = signal({ help: '', httpResponse: '', message: '' });
}
