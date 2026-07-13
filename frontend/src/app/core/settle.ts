/**
 * Camunda 8's RDBMS secondary storage is eventually consistent (flush interval
 * ~0.5s). After a write (start instance, complete task), wait briefly before
 * reading lists so the change is visible.
 */
export function settle(ms = 1500): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
