import { useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, Polyline, CircleMarker, Marker, Popup, Tooltip, useMap } from 'react-leaflet';
import L from 'leaflet';
import { CROWD, ROUTE_COLORS } from '../api';

const COIMBATORE = [11.0168, 76.9558];

// Cache icons: a new icon object makes Leaflet rebuild the DOM node, which would cancel the glide animation.
const iconCache = new Map();
function busIcon(routeNumber, crowdLevel) {
  const key = `${routeNumber}|${crowdLevel}`;
  if (!iconCache.has(key)) {
    const color = CROWD[crowdLevel]?.color ?? '#10242b';
    iconCache.set(
      key,
      L.divIcon({
        className: 'bus-marker',
        html: `<div class="bus-pin" style="background:${color}">${routeNumber}</div>`,
        iconSize: [44, 24],
        iconAnchor: [22, 12],
      })
    );
  }
  return iconCache.get(key);
}

function FitTo({ points }) {
  const map = useMap();
  useEffect(() => {
    if (points.length > 1) map.fitBounds(points, { padding: [50, 50], maxZoom: 15 });
  }, [points, map]);
  return null;
}

/**
 * Props
 *  routes     : route list (drawn as thin coloured lines)
 *  buses      : live bus positions
 *  highlight  : [{ path: [[lat,lng],...] }] journey legs to emphasise
 *  stops      : all stops (small dots)
 *  focusStops : ids of the chosen origin / destination
 */
export default function LiveMapComponent({ routes = [], buses = [], stops = [], highlight = [], focusStops = [] }) {
  const highlightPoints = useMemo(() => highlight.flatMap((h) => h.path), [highlight]);
  const dimmed = highlight.length > 0;

  return (
    <MapContainer center={COIMBATORE} zoom={12} scrollWheelZoom className="h-full w-full" aria-label="Live bus map">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {routes.map((r, i) => (
        <Polyline
          key={r.id}
          positions={r.stops.map((s) => [s.latitude, s.longitude])}
          pathOptions={{ color: ROUTE_COLORS[i % ROUTE_COLORS.length], weight: dimmed ? 2 : 4, opacity: dimmed ? 0.35 : 0.8 }}
        />
      ))}

      {highlight.map((h, i) => (
        <span key={i}>
          <Polyline positions={h.path} pathOptions={{ color: '#0b1a20', weight: 11, opacity: 0.9 }} />
          <Polyline positions={h.path} pathOptions={{ color: '#ffb81c', weight: 6, opacity: 1 }} />
        </span>
      ))}

      {stops.map((s) => {
        const chosen = focusStops.includes(s.id);
        return (
          <CircleMarker
            key={s.id}
            center={[s.latitude, s.longitude]}
            radius={chosen ? 9 : 5}
            pathOptions={{ color: '#0b1a20', weight: chosen ? 4 : 2, fillColor: chosen ? '#ffb81c' : '#fff', fillOpacity: 1 }}
          >
            <Tooltip direction="top" offset={[0, -6]}>{s.name}</Tooltip>
          </CircleMarker>
        );
      })}

      {buses.map((b) => (
        <Marker key={b.id} position={[b.latitude, b.longitude]} icon={busIcon(b.routeNumber, b.crowdLevel)}>
          <Popup>
            <strong>{b.routeNumber}</strong> to {b.towards}
            <br />
            {b.registrationNo} · {b.busTypeLabel}
            <br />
            {b.passengersOnboard}/{b.capacity} onboard ({CROWD[b.crowdLevel]?.label})
          </Popup>
        </Marker>
      ))}

      <FitTo points={highlightPoints} />
    </MapContainer>
  );
}
