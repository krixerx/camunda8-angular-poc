import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { ApiService } from '../core/api.service';
import { LanguageService } from '../core/language.service';
import { ServiceCatalogItem } from '../core/models';
import { ServicesPage } from './services-page';

const items: ServiceCatalogItem[] = [
  {
    processDefinitionKey: 1,
    processDefinitionId: 'vehicle-registration',
    name: 'Vehicle registration process',
    version: 2,
    title: 'Vehicle registration',
    summary: 'Register a car, motorcycle, or truck in your name.',
    instructions: 'Fill in the details.',
    whatYouNeed: '- VIN',
    expectedDuration: '1-2 working days',
  },
  {
    processDefinitionKey: 2,
    processDefinitionId: 'business-registration',
    name: 'Business registration process',
    version: 1,
    title: null,
    summary: null,
    instructions: null,
    whatYouNeed: null,
    expectedDuration: null,
  },
];

describe('ServicesPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServicesPage],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: { services: () => Promise.resolve(items) } },
      ],
    }).compileComponents();
  });

  it('renders CMS title and summary when the item has content', async () => {
    const fixture = TestBed.createComponent(ServicesPage);
    await fixture.whenStable();
    const cards = fixture.nativeElement.querySelectorAll('.card');
    expect(cards[0].querySelector('h2')?.textContent).toContain('Vehicle registration');
    expect(cards[0].textContent).toContain('Register a car, motorcycle, or truck in your name.');
    expect(cards[0].textContent).toContain('1-2 working days');
  });

  it('falls back to the engine name when the item has no content', async () => {
    const fixture = TestBed.createComponent(ServicesPage);
    await fixture.whenStable();
    const cards = fixture.nativeElement.querySelectorAll('.card');
    expect(cards[1].querySelector('h2')?.textContent).toContain('Business registration process');
    expect(cards[1].querySelector('a.button')).toBeTruthy();
  });

  it('renders Arabic chrome strings when the language is Arabic', async () => {
    const lang = TestBed.inject(LanguageService);
    lang.set('ar');
    const fixture = TestBed.createComponent(ServicesPage);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toContain('الخدمات');
    expect(el.querySelector('a.button')?.textContent).toContain('بدء');
    lang.set('en');
  });
});
