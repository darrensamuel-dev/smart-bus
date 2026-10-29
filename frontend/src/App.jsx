import { useEffect, useMemo, useState } from 'react';
import { api } from './api';
import LiveMapComponent from './components/LiveMapComponent';
import JourneyPlanner from './components/JourneyPlanner';
import AdminDashboard from './components/AdminDashboard';

export default function App() {
  const [view, setView] = useState('passenger');
  const [stops, setStops] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [buses, setBuses] = useState([]);
  const [online, setOnline] = useState(true);
  const [updated, setUpdated] = useState(null);
  const [option, setOption] = useState(null);
  const [focusStops, setFocusStops] = useState([]);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    Promise.all([api.stops(), api.routes()])
      .then(([s, r]) => { setStops(s); setRoutes(r); })
      .catch((e) => setLoadError(e.message));
  }, []);

  // Poll live positions every 3 seconds
  useEffect(() => {
    let alive = true;
    const load = async () => {
      try {
        const b = await api.liveBuses();
        if (!alive) return;
        setBuses(b); setOnline(true); setUpdated(new Date());
      } catch {
        if (alive) setOnline(false);
      }
    };
    load();
    const t = setInterval(load, 3000);
    return () => { alive = false; clearInterval(t); };
  }, []);

  const highlight = useMemo(() => (option ? option.legs.map((l) => ({ path: l.path })) : []), [option]);

  const tab = (id, label) => (
    <button
      onClick={() => setView(id)}
      aria-pressed={view === id}
      className={`display px-4 py-2 text-lg ${view === id ? 'bg-amber text-board' : 'text-white/80 hover:text-white'}`}
    >
      {label}
    </button>
  );

  return (
    <div className="flex h-full flex-col">
      <header className="flex flex-wrap items-center justify-between gap-2 bg-board px-4 py-2 text-white">
        <div className="flex items-baseline gap-3">
          <h1 className="display text-3xl text-amber">TransitIQ</h1>
          <span className="hidden text-sm text-white/70 sm:inline">SmartBus · Coimbatore</span>
        </div>
        <nav className="flex gap-1" aria-label="Views">
          {tab('passenger', 'Passenger')}
          {tab('ops', 'Operations')}
        </nav>
        <p className="text-sm" role="status">
          {online
            ? <>Live{updated && <span className="text-white/70"> · updated {updated.toLocaleTimeString()}</span>}</>
            : <span className="text-amber">Backend offline. Start the Spring Boot server on port 8080.</span>}
        </p>
      </header>

      {loadError && <p role="alert" className="bg-signal px-4 py-2 text-white">Could not load stops and routes: {loadError}</p>}

      {view === 'passenger' ? (
        <main className="grid min-h-0 flex-1 grid-rows-[auto_minmax(0,1fr)] lg:grid-cols-[420px_minmax(0,1fr)] lg:grid-rows-1">
          <aside className="max-h-[55vh] border-r-2 border-ink lg:max-h-none">
            <JourneyPlanner stops={stops} onSelectOption={setOption} onSearchStops={setFocusStops} />
          </aside>
          <section className="min-h-[50vh]">
            <LiveMapComponent routes={routes} stops={stops} buses={buses} highlight={highlight} focusStops={focusStops} />
          </section>
        </main>
      ) : (
        <main className="min-h-0 flex-1"><AdminDashboard /></main>
      )}
    </div>
  );
}
