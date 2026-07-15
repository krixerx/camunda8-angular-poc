'use strict';

/**
 * Same guard as the service content type: the public find permission must only
 * ever expose published translations, so the status override is pinned here.
 */
module.exports = () => async (ctx, next) => {
  ctx.query = { ...ctx.query, status: 'published' };
  await next();
};
