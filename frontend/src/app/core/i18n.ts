export type Locale = 'en' | 'ar';

/**
 * App chrome strings (nav, buttons, empty states, error fallbacks). Developer-owned
 * by design: chrome changes on code cadence, so it lives in the repo — editorial
 * content and form labels are translated in Strapi instead.
 */
export const MESSAGES = {
  'nav.services': { en: 'Services', ar: 'الخدمات' },
  'nav.tasks': { en: 'Tasks', ar: 'المهام' },
  'nav.processes': { en: 'Processes', ar: 'العمليات' },
  'nav.logout': { en: 'Log out', ar: 'تسجيل الخروج' },
  'role.applicant': { en: 'applicant', ar: 'مقدم طلب' },
  'role.civilServant': { en: 'civil servant', ar: 'موظف حكومي' },

  'services.title': { en: 'Services', ar: 'الخدمات' },
  'services.start': { en: 'Start', ar: 'بدء' },
  'services.empty': { en: 'No processes deployed.', ar: 'لا توجد عمليات منشورة.' },
  'services.loadFailed': { en: 'Failed to load services', ar: 'تعذر تحميل الخدمات' },

  'start.back': { en: '← Services', ar: '→ الخدمات' },
  'start.title': { en: 'Start:', ar: 'بدء:' },
  'start.whatYouNeed': { en: 'What you need', ar: 'ما تحتاج إليه' },
  'start.noForm': {
    en: 'This process has no start form.',
    ar: 'لا يوجد نموذج بدء لهذه العملية.',
  },
  'start.submit': { en: 'Start process', ar: 'بدء العملية' },
  'start.failed': { en: 'Failed to start process', ar: 'تعذر بدء العملية' },
  'start.formLoadFailed': { en: 'Failed to load start form', ar: 'تعذر تحميل نموذج البدء' },

  'tasks.title': { en: 'Tasks', ar: 'المهام' },
  'tasks.refresh': { en: 'Refresh', ar: 'تحديث' },
  'tasks.open': { en: 'Open', ar: 'فتح' },
  'tasks.created': { en: 'created', ar: 'أُنشئت' },
  'tasks.empty': { en: 'No open tasks.', ar: 'لا توجد مهام مفتوحة.' },
  'tasks.loadFailed': { en: 'Failed to load tasks', ar: 'تعذر تحميل المهام' },

  'task.back': { en: '← Tasks', ar: '→ المهام' },
  'task.variables': { en: 'Variables', ar: 'المتغيرات' },
  'task.complete': { en: 'Complete task', ar: 'إكمال المهمة' },
  'task.loadFailed': { en: 'Failed to load task', ar: 'تعذر تحميل المهمة' },
  'task.completeFailed': { en: 'Failed to complete task', ar: 'تعذر إكمال المهمة' },

  'processes.title': { en: 'Processes', ar: 'العمليات' },
  'processes.process': { en: 'Process', ar: 'العملية' },
  'processes.state': { en: 'State', ar: 'الحالة' },
  'processes.started': { en: 'Started', ar: 'تاريخ البدء' },
  'processes.ended': { en: 'Ended', ar: 'تاريخ الانتهاء' },
  'processes.empty': { en: 'No process instances.', ar: 'لا توجد عمليات.' },
  'processes.loadFailed': {
    en: 'Failed to load process instances',
    ar: 'تعذر تحميل العمليات',
  },
} as const satisfies Record<string, Record<Locale, string>>;

export type MessageKey = keyof typeof MESSAGES;
