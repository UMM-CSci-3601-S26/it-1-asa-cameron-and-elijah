import { Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { RouterLink } from '@angular/router';
import { Supply } from './supply';

@Component({
  selector: 'app-supply-card',
  templateUrl: './supply-card.component.html',
  styleUrls: ['./supply-card.component.scss'],
  imports: [MatCardModule, MatButtonModule, MatListModule, MatIconModule, RouterLink]
})
export class SupplyCardComponent {

  supply = input.required<Supply>();
  simple = input(false);
}
