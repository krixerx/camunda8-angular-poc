'use strict';

const { createCoreRouter } = require('@strapi/strapi').factories;

module.exports = createCoreRouter('api::form-translation.form-translation', {
  config: {
    find: { middlewares: ['api::form-translation.force-published'] },
    findOne: { middlewares: ['api::form-translation.force-published'] },
  },
});
