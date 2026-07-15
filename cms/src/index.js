'use strict';

const seedServices = require('./data/seed-services.json');

const SERVICE_UID = 'api::service.service';
const PUBLIC_ACTIONS = [`${SERVICE_UID}.find`, `${SERVICE_UID}.findOne`];

/** Grant the public role read access to the service content type (idempotent). */
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

/** Seed and publish catalog entries when the content type is empty (idempotent). */
async function seedCatalog(strapi) {
  const count = await strapi.db.query(SERVICE_UID).count();
  if (count > 0) {
    strapi.log.info(`cms bootstrap: ${count} service entries exist, skipping seed`);
    return;
  }
  for (const data of seedServices) {
    await strapi.documents(SERVICE_UID).create({ data, status: 'published' });
    strapi.log.info(`cms bootstrap: seeded service '${data.processDefinitionId}'`);
  }
}

module.exports = {
  register() {},

  async bootstrap({ strapi }) {
    await grantPublicRead(strapi);
    await seedCatalog(strapi);
  },
};
