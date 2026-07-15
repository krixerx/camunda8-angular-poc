'use strict';

/**
 * The content API would happily serve draft versions to anyone holding the
 * public find permission (`?status=draft`); the catalog must only ever expose
 * published content, so the status override is pinned here.
 */
module.exports = () => async (ctx, next) => {
  ctx.query = { ...ctx.query, status: 'published' };
  await next();
};
