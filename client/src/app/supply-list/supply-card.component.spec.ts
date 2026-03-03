// import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
// import { SupplyCardComponent } from './supply-card.component';
// import { Supply } from './supply';

// describe('SupplyCardComponent', () => {
//   let component: SupplyCardComponent;
//   let fixture: ComponentFixture<SupplyCardComponent>;
//   let expectedSupply: Supply;

//   beforeEach(waitForAsync(() => {
//     TestBed.configureTestingModule({
//       imports: [
//         SupplyCardComponent
//       ]
//     })
//       .compileComponents();
//   }));

  // beforeEach(() => {
  //   fixture = TestBed.createComponent(SupplyCardComponent);
  //   component = fixture.componentInstance;
  //   expectedSupply = {
  //     _id: 'chris_id',
  //     name: 'Chris',
  //     age: 25,
  //     company: 'UMM',
  //     email: 'chris@this.that',
  //     role: 'admin',
  //     avatar: 'https://gravatar.com/avatar/8c9616d6cc5de638ea6920fb5d65fc6c?d=identicon'
  //   };
  //   fixture.componentRef.setInput('supply', expectedSupply);
  //   fixture.detectChanges();
  // });

//   it('should create', () => {
//     expect(component).toBeTruthy();
//   });

//   it('should be associated with the correct supply', () => {
//     expect(component.supply()).toEqual(expectedSupply);
//   });

//   it('should be the supply named Chris', () => {
//     expect(component.supply().description).toEqual('Chris');
//   });
// });
