export function localDate(timeZone, date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(date);
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${values.year}-${values.month}-${values.day}`;
}

export class DailyResetScheduler {
  constructor(store, timeZone, now = () => new Date()) {
    this.store = store;
    this.timeZone = timeZone;
    this.now = now;
  }

  initialize() {
    this.store.initializeUsageDate(localDate(this.timeZone, this.now()));
  }

  check() {
    return this.store.resetForDate(localDate(this.timeZone, this.now()));
  }
}
