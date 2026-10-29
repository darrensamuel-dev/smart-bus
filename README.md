# TransitIQ — SmartBus
**An Object-Oriented Public Transport Operations and Passenger Demand Platform** (demo city: Coimbatore)

Passengers plan journeys and watch buses move live on a map. Operators see how many riders are expected on each route this hour and how many buses are needed to carry them.

## What makes it different
| Feature | How it works |
|---|---|
| Live bus map | A GPS simulator moves every running bus along its route every 3 s. Each bus type has its own speed. |
| Crowd-aware journey planner | Direct and one-change journeys. Each leg shows the next real bus, its ETA, fare (AC and Express cost more) and how full it is. |
| Explainable demand forecast | Recency-weighted average of past boardings for the same route, hour and day type, plus a live surge factor when buses are already over 80% full. |
| Fleet advice | Compares the forecast with the seats running buses can supply per hour and recommends adding buses, counting idle reserve buses. |

## Project layout
```
TransitIQ/
├── backend/    Spring Boot 3 · Java 17 · JPA · MySQL · REST
├── frontend/   React 18 · Vite · Tailwind CSS 4 · Leaflet
└── database/   seed_routes.sql (+ generate_seed.py that creates it)
```

## OOP concepts used (for your viva)
- **Abstraction**: `User` is abstract; it can only exist as `Passenger` or `TransportAdmin`.
- **Inheritance**: `Passenger` and `TransportAdmin` extend `User` (JPA JOINED inheritance, one table each).
- **Polymorphism**: `User.getRole()` and `BusType.travelMinutes()` behave differently per subclass/constant. No if/else on type anywhere.
- **Encapsulation**: `Bus.board()` never exceeds capacity; `Passenger.deductFare()` guards the wallet; `Route` owns its own geometry (`positionAt`, `metersAt`, `pathBetween`).
- **Composition / association**: `Route` has ordered `BusStop`s; `Bus` belongs to a `Route`; `Trip` links a `Bus` and a `Route`.
- **Layered architecture**: controller → service → repository → model; entities are converted to DTOs before leaving the service layer.

## Run it

### 1. Prerequisites
Java 17+, Maven 3.9+, MySQL 8, Node 18+.

### 2. Backend
```bash
cd backend
# optional: set your MySQL login (defaults: root / root)
export DB_USER=root
export DB_PASSWORD=yourpassword
mvn spring-boot:run
```
On first start Hibernate creates the `smartbus` database tables, then `../database/seed_routes.sql` is loaded automatically
(18 stops, 7 routes, 23 buses, 1,064 hourly demand records). The script is safe to run on every restart.
If you start the app from the project root instead of `backend/`, the `optional:` paths in `application.properties` still find the file.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. The Vite dev server proxies `/api` to port 8080.

## REST API
| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/stops` | All stops |
| GET | `/api/routes`, `/api/routes/{id}` | Routes with ordered stops |
| GET | `/api/buses`, `/api/buses/live` | Whole fleet / buses in service with positions |
| GET | `/api/journey?from={stopId}&to={stopId}` | Journey options (direct + one change) |
| GET | `/api/trips/recent` | Latest 15 trips |
| GET | `/api/demand/forecast?routeId=` | Hourly forecast and bus recommendation for a route |
| GET | `/api/demand/insights` | Network KPIs + forecast for every route |
| POST | `/api/auth/register` | Create a passenger (or admin with the invite code in `application.properties`) |
| POST | `/api/auth/login` | Check credentials (BCrypt) |

## How the forecast works
1. Collect history for the same **route + hour + day type** (weekday or weekend).
2. Average with weight `1 / (1 + daysOld / 7)`, so recent weeks count more.
3. Multiply by `1 + (avgOccupancy - 80) / 100` when running buses are above 80% full.
4. Supply per bus per hour = `seats × (60 / one-way trip minutes) × 0.85`.
5. If expected riders exceed supply, status is **Add buses**; above 70% of supply it is **Near capacity**.

## Make it real
- Replace `BusTrackingService.tick()` with an endpoint that receives GPS pings from devices and calls `Bus.moveTo()`.
- Import real ticketing data into `demand_records` (restart the app to clear the cached curves).
- Add JWT login and protect `/api/demand/**` for admins only.
- Add road-following geometry (OSRM) instead of straight lines between stops.

## Known limits
- Bus positions are simulated, not real GPS.
- Login endpoints exist but the UI does not use sessions or tokens yet.
- Journey search supports at most one change.
