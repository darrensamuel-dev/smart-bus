import { useEffect, useState } from 'react';
import { api } from '../api';
import RouteChip from './RouteChip';

const STATUS = {
  OK: { label: 'Sufficient', cls: 'bg-moss text-white' },
  WATCH: { label: 'Near capacity', cls: 'bg-amber text-board' },
  ADD_BUSES: { label: 'Add buses', cls: 'bg-signal text-white' },
};

function Kpi({ value, label }) {
  return (
    <div className="px-5 py-3 first:pl-0">
      <div className="display text-4xl leading-none">{value}</div>
      <div className="mt-1 text-sm text-ink/70">{label}</div>
    </div>
  );
}

function HourlyChart({ route }) {
  const max = Math.max(route.peakPredicted, route.capacityNow, 1) * 1.1;
  const pct = (v) => `${Math.min(100, (v / max) * 100)}%`;
  return (
    <figure>
      <figcaption className="mb-2 text-sm text-ink/70">
        Expected riders per hour on route {route.routeNumber} ({route.dayType.toLowerCase()}). The dashed line is what the buses running now can carry.
      </figcaption>
      <div className="relative flex h-52 items-end gap-1 border-b-2 border-ink">
        <div className="absolute inset-x-0 border-t-2 border-dashed border-board" style={{ bottom: pct(route.capacityNow) }} />
        {route.hours.map((h) => {
          const over = h.predicted > route.capacityNow;
          const now = h.hour === route.currentHour;
          return (
            <div key={h.hour} className="flex h-full flex-1 flex-col justify-end" title={`${h.hour}:00 · ${h.predicted} riders · ${h.requiredBuses} buses needed`}>
              <div className={`w-full ${over ? 'bg-signal' : 'bg-moss'} ${now ? 'outline-3 outline-amber outline-offset-1' : ''}`} style={{ height: pct(h.predicted) }} />
            </div>
          );
        })}
      </div>
      <div className="mt-1 flex gap-1 text-xs text-ink/70">
        {route.hours.map((h) => <span key={h.hour} className="flex-1 text-center">{h.hour}</span>)}
      </div>
    </figure>
  );
}

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    let alive = true;
    const load = () => api.insights()
      .then((d) => { if (alive) { setData(d); setError(''); setSelectedId((id) => id ?? d.routes[0]?.routeId ?? null); } })
      .catch((e) => alive && setError(e.message));
    load();
    const t = setInterval(load, 8000);
    return () => { alive = false; clearInterval(t); };
  }, []);

  if (error && !data) return <p role="alert" className="p-6 text-signal">Cannot reach the backend: {error}</p>;
  if (!data) return <p className="p-6">Loading operations data…</p>;

  const selected = data.routes.find((r) => r.routeId === selectedId) ?? data.routes[0];
  const k = data.kpis;

  return (
    <div className="mx-auto h-full max-w-6xl overflow-y-auto p-5">
      <div className="flex flex-wrap divide-x-2 divide-ink/20 border-b-2 border-ink pb-3">
        <Kpi value={k.runningBuses} label="buses in service" />
        <Kpi value={k.idleBuses} label="buses in reserve" />
        <Kpi value={k.passengersOnboard} label="riders onboard now" />
        <Kpi value={`${k.avgOccupancyPercent}%`} label="average fullness" />
        <Kpi value={k.alerts} label={k.alerts === 1 ? 'route needs buses' : 'routes need buses'} />
        <Kpi value={k.tripsCompleted} label="trips completed" />
      </div>

      <div className="mt-5 grid gap-6 lg:grid-cols-[1.25fr_1fr]">
        <section aria-label="Routes">
          <h2 className="display mb-2 text-2xl">This hour, by route</h2>
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b-2 border-ink">
                <th className="py-2 pr-2">Route</th>
                <th className="py-2 pr-2">Expected riders</th>
                <th className="py-2 pr-2">Can carry</th>
                <th className="py-2 pr-2">Buses</th>
                <th className="py-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.routes.map((r) => (
                <tr key={r.routeId} onClick={() => setSelectedId(r.routeId)} tabIndex={0}
                  onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && setSelectedId(r.routeId)}
                  className={`cursor-pointer border-b border-mist ${selected.routeId === r.routeId ? 'bg-white' : 'hover:bg-white/60'}`}>
                  <td className="py-2 pr-2"><RouteChip number={r.routeNumber} /></td>
                  <td className="py-2 pr-2 font-semibold">{r.predictedNow}</td>
                  <td className="py-2 pr-2">{r.capacityNow}</td>
                  <td className="py-2 pr-2">{r.activeBuses} of {r.recommendedBuses} needed</td>
                  <td className="py-2"><span className={`board rounded-sm px-2 py-1 text-xs ${STATUS[r.status].cls}`}>{STATUS[r.status].label}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section aria-label="Selected route forecast" className="border-2 border-ink bg-white p-4">
          <h2 className="display text-2xl">{selected.routeNumber}: {selected.routeName}</h2>
          <p className="mb-3 mt-1">{selected.advice}</p>
          <HourlyChart route={selected} />
          <p className="mt-3 text-sm text-ink/70">Busiest hour: {selected.peakHour}:00 with about {selected.peakPredicted} riders.</p>
        </section>
      </div>
    </div>
  );
}
