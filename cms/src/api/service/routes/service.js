'use strict';

const { createCoreRouter } = require('@strapi/strapi').factories;

module.exports = createCoreRouter('api::service.service', {
  config: {
    find: { middlewares: ['api::service.force-published'] },
    findOne: { middlewares: ['api::service.force-published'] },
  },
});
