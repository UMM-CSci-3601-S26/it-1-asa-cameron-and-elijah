import { Component, signal, inject, Signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { catchError, map, switchMap } from 'rxjs/operators';
import { SupplyCardComponent } from './supply-card.component';
import { SupplyService } from './supply.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { Supply } from './supply';

@Component({
  selector: 'app-supply-request',
  templateUrl: './supply-request.component.html',
  styleUrls: ['./supply-request.component.scss'],
  imports: [SupplyCardComponent, MatCardModule],
})
export class SupplyRequestComponent {
  private route = inject(ActivatedRoute);
  private supplyService = inject(SupplyService);

  supply: Signal<Supply> = toSignal(
    this.route.paramMap.pipe(
      // Map the paramMap into the id
      map((paramMap: ParamMap) => paramMap.get('id')),
      // Maps the `id` string into the Observable<User>,
      // which will emit zero or one values depending on whether there is a
      // `User` with that ID.
      switchMap((id: string) => this.supplyService.getSupplyById(id)),
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
