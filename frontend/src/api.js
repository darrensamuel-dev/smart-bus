// All HTTP calls live here. In dev, Vite proxies /api to http://localhost:8080.
const BASE = import.meta.env.VITE_API_URL ?? '/api';

async function get(path) {
  const res = await fetch(BASE + path);
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body.error) message = body.error;
    } catch { /* body was not JSON */ }
    throw new Error(message);
  }
  return res.json();
}

export const api = {
  stops: () => get('/stops'),
  routes: () => get('/routes'),
  liveBuses: () => get('/buses/live'),
  journey: (from, to) => get(`/journey?from=${from}&to=${to}`),
  forecast: (routeId) => get(`/demand/forecast?routeId=${routeId}`),
  insights: () => get('/demand/insights'),
  recentTrips: () => get('/trips/recent'),
};

export const CROWD = {
  SEATS_AVAILABLE: { label: 'Seats available', color: '#1f6f5b' },
  STANDING_ROOM: { label: 'Standing room', color: '#b87d00' },
  CROWDED: { label: 'Crowded', color: '#d6402f' },
};

export const ROUTE_COLORS = ['#1f6f5b', '#2f5fa8', '#b5533c', '#7a4fa0', '#a37b00', '#00808a', '#8a4a2b'];
