'use strict';

const seedServices = require('./data/seed-services.json');
const seedServicesAr = require('./data/seed-services.ar.json');
const seedFormTranslations = require('./data/seed-form-translations.json');

const SERVICE_UID = 'api::service.service';
const FORM_TRANSLATION_UID = 'api::form-translation.form-translation';
const PUBLIC_ACTIONS = [
  `${SERVICE_UID}.find`,
  `${SERVICE_UID}.findOne`,
  `${FORM_TRANSLATION_UID}.find`,
  `${FORM_TRANSLATION_UID}.findOne`,
];

/** Grant the public role read access to the content types (idempotent). */
async function grantPublicRead(strapi) {
  const publicRole = await strapi.db
    .query('plugin::users-permissions.role')
    .findOne({ where: { type: 'public' } });
  if (!publicRole) {
    strapi.log.warn('cms bootstrap: public role not found, skipping permission grant');
    return;
  }
  for (const action of PUBLIC_ACTIONS) {
    const existing = await strapi.db
      .query('plugin::users-permissions.permission')
      .findOne({ where: { action, role: publicRole.id } });
    if (!existing) {
      await strapi.db
        .query('plugin::users-permissions.permission')
        .create({ data: { action, role: publicRole.id } });
      strapi.log.info(`cms bootstrap: granted public ${action}`);
    }
  }
}

/** Register the Arabic locale so localized documents can be created (idempotent). */
async function ensureArabicLocale(strapi) {
  const existing = await strapi.db
    .query('plugin::i18n.locale')
    .findOne({ where: { code: 'ar' } });
  if (!existing) {
    await strapi.plugin('i18n').service('locales').create({ code: 'ar', name: 'Arabic (ar)' });
    strapi.log.info('cms bootstrap: created locale ar');
  }
}

/**
 * Seed and publish catalog entries per locale (idempotent, per document):
 * English documents are created when missing; Arabic localizations are added to
 * documents that lack them — which also upgrades volumes seeded before i18n.
 */
async function seedCatalog(strapi) {
  const arByProcessId = new Map(seedServicesAr.map((s) => [s.processDefinitionId, s]));

  for (const data of seedServices) {
    let doc = await strapi.db
      .query(SERVICE_UID)
      .findOne({ where: { processDefinitionId: data.processDefinitionId } });
    if (!doc) {
      doc = await strapi.documents(SERVICE_UID).create({ data, status: 'published' });
      strapi.log.info(`cms bootstrap: seeded service '${data.processDefinitionId}' (en)`);
    }

    const arData = arByProcessId.get(data.processDefinitionId);
    if (!arData) {
      continue;
    }
    const arVersion = await strapi
      .documents(SERVICE_UID)
      .findOne({ documentId: doc.documentId, locale: 'ar' });
    if (!arVersion) {
      const { processDefinitionId, ...localizedFields } = arData;
      await strapi.documents(SERVICE_UID).update({
        documentId: doc.documentId,
        locale: 'ar',
        data: localizedFields,
        status: 'published',
      });
      strapi.log.info(`cms bootstrap: seeded service '${processDefinitionId}' (ar)`);
    }
  }
}

/**
 * Seed and publish Arabic form translations (idempotent, per form). English is
 * authored in the deployed .form files, so only 'ar' documents are created.
 */
async function seedFormTranslationEntries(strapi) {
  for (const { formId, strings } of seedFormTranslations) {
    const existing = await strapi.db
      .query(FORM_TRANSLATION_UID)
      .findOne({ where: { formId } });
    if (!existing) {
      await strapi.documents(FORM_TRANSLATION_UID).create({
        locale: 'ar',
        data: { formId, strings },
        status: 'published',
      });
      strapi.log.info(`cms bootstrap: seeded form translation '${formId}' (ar)`);
    }
  }
}

module.exports = {
  register() {},

  async bootstrap({ strapi }) {
    await grantPublicRead(strapi);
    await ensureArabicLocale(strapi);
    await seedCatalog(strapi);
    await seedFormTranslationEntries(strapi);
  },
};
