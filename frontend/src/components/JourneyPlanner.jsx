import { useState } from 'react';
import { api, CROWD } from '../api';
import RouteChip from './RouteChip';

export default function JourneyPlanner({ stops, onSelectOption, onSearchStops }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [result, setResult] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const swap = () => { setFrom(to); setTo(from); };

  async function search(e) {
    e.preventDefault();
    setError(''); setLoading(true); setResult(null); setSelected(null);
    try {
      const data = await api.journey(from, to);
      setResult(data);
      onSearchStops([data.from.id, data.to.id]);
      if (data.options.length) { setSelected(0); onSelectOption(data.options[0]); }
      else onSelectOption(null);
    } catch (err) {
      setError(err.message);
      onSelectOption(null);
    } finally {
      setLoading(false);
    }
  }

  const select = (i) => { setSelected(i); onSelectOption(result.options[i]); };

  const stopSelect = (id, value, set, label) => (
    <label className="block">
      <span className="mb-1 block text-sm font-medium">{label}</span>
      <select
        id={id}
        value={value}
        onChange={(e) => set(e.target.value)}
        required
        className="w-full rounded-sm border-2 border-ink/80 bg-white px-3 py-2 text-base"
      >
        <option value="" disabled>Choose a stop</option>
        {stops.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
      </select>
    </label>
  );

  return (
    <div className="flex h-full flex-col">
      <form onSubmit={search} className="space-y-3 border-b-2 border-ink bg-white p-4">
        <h2 className="display text-2xl leading-none">Where to?</h2>
        {stopSelect('from', from, setFrom, 'From')}
        <div className="flex items-end gap-2">
          <div className="flex-1">{stopSelect('to', to, setTo, 'To')}</div>
          <button type="button" onClick={swap} aria-label="Swap start and destination"
            className="h-[42px] rounded-sm border-2 border-ink px-3 text-lg hover:bg-mist">⇅</button>
        </div>
        <button disabled={loading || !from || !to}
          className="display w-full rounded-sm bg-board px-4 py-2.5 text-lg text-amber disabled:opacity-40">
          {loading ? 'Finding buses…' : 'Find buses'}
        </button>
      </form>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {error && <p role="alert" className="rounded-sm border-2 border-signal bg-white p-3 text-signal">{error}</p>}

        {!result && !error && (
          <p className="text-ink/70">Pick where you are and where you are going. You will see the next live bus, how full it is, and the fare.</p>
        )}

        {result && result.options.length === 0 && (
          <p className="rounded-sm border-2 border-ink bg-white p-3">
            No bus connects {result.from.name} and {result.to.name} with one change or fewer. Try a nearby stop.
          </p>
        )}

        {result && result.options.length > 0 && (
          <ul className="space-y-3">
            {result.options.map((o, i) => (
              <li key={i}>
                <button onClick={() => select(i)} aria-pressed={selected === i}
                  className={`w-full rounded-sm border-2 p-3 text-left ${selected === i ? 'border-ink bg-white shadow-[4px_4px_0_var(--color-amber)]' : 'border-mist bg-white/70 hover:border-ink/60'}`}>
                  <div className="flex items-baseline justify-between">
                    <span className="display text-3xl">{o.totalMinutes} min</span>
                    <span className="text-lg font-semibold">₹{o.totalFare}</span>
                  </div>
                  <p className="mb-2 text-sm text-ink/70">
                    {o.transfers === 0 ? 'Direct bus' : `Change at ${o.legs[0].alightStop}`}
                  </p>
                  <ol className="space-y-2">
                    {o.legs.map((l, k) => (
                      <li key={k} className="border-l-4 border-board pl-3">
                        <div className="flex items-center gap-2">
                          <RouteChip number={l.routeNumber} />
                          <span className="text-sm">{l.boardStop} to {l.alightStop}</span>
                        </div>
                        <p className="mt-1 text-sm text-ink/80">
                          {l.stopsCount} stops · {l.distanceKm} km · {l.rideMinutes} min · ₹{l.fare}
                        </p>
                        {l.nextBusEtaMinutes != null ? (
                          <p className="text-sm">
                            Next bus in <strong>{l.nextBusEtaMinutes} min</strong> ({l.nextBusType}, {l.nextBusReg}){' '}
                            <span className="font-semibold" style={{ color: CROWD[l.crowdLevel]?.color }}>
                              {CROWD[l.crowdLevel]?.label}
                            </span>
                          </p>
                        ) : (
                          <p className="text-sm text-ink/70">No bus running on this route right now.</p>
                        )}
                      </li>
                    ))}
                  </ol>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
