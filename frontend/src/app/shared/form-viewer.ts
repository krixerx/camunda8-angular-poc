import {
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  output,
  viewChild,
} from '@angular/core';
import { Form } from '@bpmn-io/form-js-viewer';
import { FormSchema } from '../core/models';

export interface FormSubmission {
  data: Record<string, unknown>;
  errors: Record<string, string[]>;
}

/**
 * Wraps the @bpmn-io/form-js viewer in an Angular component.
 * Renders a Camunda Form schema, prefilled with `data`. The host page calls
 * submit(); validation errors keep the submission local (submitted never fires).
 */
@Component({
  selector: 'app-form-viewer',
  template: '<div #container></div>',
})
export class FormViewer implements OnDestroy {
  readonly schema = input.required<FormSchema>();
  readonly data = input<Record<string, unknown>>({});
  readonly submitted = output<Record<string, unknown>>();

  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('container');
  private form: Form | null = null;

  constructor() {
    effect(() => {
      const schema = this.schema();
      const data = this.data();
      const element = this.container().nativeElement;
      void this.render(element, schema, data);
    });
  }

  ngOnDestroy(): void {
    this.form?.destroy();
    this.form = null;
  }

  /** Validates and submits the form; emits `submitted` and returns the data when valid. */
  submit(): Record<string, unknown> | null {
    if (!this.form) {
      return null;
    }
    const { data, errors } = this.form.submit() as FormSubmission;
    if (errors && Object.keys(errors).length > 0) {
      return null; // form-js renders the field errors itself
    }
    this.submitted.emit(data);
    return data;
  }

  private async render(
    element: HTMLDivElement,
    schema: FormSchema,
    data: Record<string, unknown>,
  ): Promise<void> {
    this.form?.destroy();
    this.form = new Form({ container: element });
    await this.form.importSchema(schema, data);
  }
}
