import { Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { RouterLink } from '@angular/router';
import { Inventory } from './inventory';

@Component({
  selector: 'app-inventory-card',
  templateUrl: './inventory-card.component.html',
  styleUrls: ['./inventory-card.component.scss'],
  imports: [MatCardModule, MatButtonModule, MatListModule, MatIconModule, RouterLink]
})
export class InventoryCardComponent {

  inventory = input.required<Inventory>();
  simple = input(false);
}
