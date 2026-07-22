// Service endpoints for the TraderX backend microservices.
// Ports mirror the Angular front-end `environment.ts` so both apps
// talk to the same services during local development.
const host = window.location.hostname;

export const environment = {
  production: false,
  accountUrl: `//${host}:18088`,
  referenceDataUrl: `//${host}:18085`,
  tradesUrl: `//${host}:18092/trade/`,
  positionsUrl: `//${host}:18090`,
  peopleUrl: `//${host}:18089`,
  tradeFeedUrl: `//${host}:18086`,
};
