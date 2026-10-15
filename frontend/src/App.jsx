import { useMemo, useState } from 'react';

const API_BASE = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`;
const SESSION_TOKEN_KEY = 'railwaygo_auth_token';
const SESSION_USER_KEY = 'railwaygo_auth_user';

function readStoredUser() {
  try {
    const value = sessionStorage.getItem(SESSION_USER_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(sessionStorage.getItem(SESSION_TOKEN_KEY)
        ? { Authorization: `Bearer ${sessionStorage.getItem(SESSION_TOKEN_KEY)}` }
        : {}),
      ...(options.headers || {}),
    },
  });
  const body = await response.text();
  let data;
  try { data = body ? JSON.parse(body) : null; } catch { data = body; }
  if (!response.ok) {
    const message = typeof data === 'string' ? data : (data?.message || data?.error || `Request failed (${response.status})`);
    throw new Error(message);
  }
  return data;
}

function timeToMinutes(value) {
  if (!value) return 0;
  const [h, m] = value.split(':').map(Number);
  return h * 60 + m;
}

function formatDuration(start, end) {
  let mins = timeToMinutes(end) - timeToMinutes(start);
  if (mins < 0) mins += 24 * 60;
  return `${Math.floor(mins / 60)}h ${mins % 60}m`;
}

const trains = [];
const cityOptions = ['Bangalore', 'Jaipur', 'Delhi', 'Mumbai', 'Patna'];
const today = new Date().toISOString().slice(0, 10);

function Icon({ name, size = 20 }) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true
  };

  const paths = {
    train: (
      <>
        <rect x="5" y="3" width="14" height="15" rx="3" />
        <path d="M8 21l2-3m6 3-2-3M5 13h14M8 7h.01M16 7h.01M8 16h.01M16 16h.01" />
      </>
    ),
    search: (
      <>
        <circle cx="10.8" cy="10.8" r="6.8" />
        <path d="m16 16 4.5 4.5" />
      </>
    ),
    pin: (
      <>
        <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
        <circle cx="12" cy="10" r="2.5" />
      </>
    ),
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M16 3v4M8 3v4M3 10h18" />
      </>
    ),
    swap: (
      <>
        <path d="M4 7h15l-3-3m3 3-3 3M20 17H5l3 3m-3-3 3-3" />
      </>
    ),
    shield: (
      <>
        <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" />
        <path d="m9 12 2 2 4-4" />
      </>
    ),
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </>
    ),
    ticket: (
      <>
        <path d="M3 8a2 2 0 0 0 0 4v4a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-4a2 2 0 0 0 0-4V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2Z" />
        <path d="M13 4v2m0 3v2m0 3v2m0 2v2" />
      </>
    ),
    arrow: (
      <>
        <path d="M5 12h14m-6-6 6 6-6 6" />
      </>
    ),
    user: (
      <>
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21a8 8 0 0 1 16 0" />
      </>
    ),
    check: <path d="m5 12 4 4L19 6" />
  };

  return <svg {...common}>{paths[name] || paths.train}</svg>;
}

function calculateSampleFare(stations, source, destination) {
  const route = (Array.isArray(stations) ? stations : []).map(station => String(station).trim().toLowerCase());
  const sourceIndex = route.indexOf(String(source || '').trim().toLowerCase());
  const destinationIndex = route.indexOf(String(destination || '').trim().toLowerCase());
  if (sourceIndex < 0 || destinationIndex <= sourceIndex) return null;
  return (destinationIndex - sourceIndex) * 150;
}

function formatRupees(value) {
  return Number.isFinite(Number(value)) && value != null ? `₹${Number(value).toLocaleString('en-IN')}` : '—';
}

function durationMinutes(duration) {
  const hours = Number(duration.match(/(\d+)h/)?.[1] || 0);
  const minutes = Number(duration.match(/(\d+)m/)?.[1] || 0);

  return hours * 60 + minutes;
}

function App() {
  const [from, setFrom] = useState('Bangalore');
  const [to, setTo] = useState('Delhi');
  const [date, setDate] = useState(today);
  const [searched, setSearched] = useState(false);
  const [trains, setTrains] = useState([]);
  const [loadingTrains, setLoadingTrains] = useState(false);
  const [seatMatrix, setSeatMatrix] = useState([]);
  const [bookingBusy, setBookingBusy] = useState(false);
  const [bookingsOpen, setBookingsOpen] = useState(false);
  const [bookings, setBookings] = useState([]);
  const [bookingsLoading, setBookingsLoading] = useState(false);
  const [selectedTrain, setSelectedTrain] = useState(null);
  const [authMode, setAuthMode] = useState('');
  const [authForm, setAuthForm] = useState({ name: '', email: '', password: '' });
  const [currentUser, setCurrentUser] = useState(readStoredUser);
  const [authBusy, setAuthBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const [sort, setSort] = useState('recommended');
  const [departureFilter, setDepartureFilter] = useState('any');
  const [availableOnly, setAvailableOnly] = useState(false);

  // Seat selection
  const [selectedSeats, setSelectedSeats] = useState([]);

  // Passenger details
  const [passengers, setPassengers] = useState([
    { name: '', age: '' }
  ]);

  // Current booking screen
  const [bookingStep, setBookingStep] = useState('seats');

  const results = useMemo(() => {
    let matches = trains;
    if (departureFilter !== 'any') {
      matches = matches.filter(train => {
        const hour = Number((train.depart || '00:00').split(':')[0]);
        if (departureFilter === 'morning') return hour >= 5 && hour < 12;
        if (departureFilter === 'afternoon') return hour >= 12 && hour < 17;
        if (departureFilter === 'evening') return hour >= 17 && hour < 21;
        return hour >= 21 || hour < 5;
      });
    }
    if (availableOnly) matches = matches.filter(train => train.seats > 0);
    if (sort === 'fare') matches = [...matches].sort((a, b) => (a.fare || 0) - (b.fare || 0));
    if (sort === 'departure') matches = [...matches].sort((a, b) => a.depart.localeCompare(b.depart));
    if (sort === 'duration') matches = [...matches].sort((a, b) => durationMinutes(a.duration) - durationMinutes(b.duration));
    return matches;
  }, [trains, sort, departureFilter, availableOnly]);

  async function searchTrains(event) {
    event.preventDefault();
    setSearched(true);
    setSelectedTrain(null);
    setTrains([]);
    setDepartureFilter('any');
    setAvailableOnly(false);
    if (from.toLowerCase() === to.toLowerCase()) {
      setNotice('Choose two different stations to search for trains.');
      return;
    }
    setLoadingTrains(true);
    setNotice('');
    try {
      const data = await apiRequest(`/trains?source=${encodeURIComponent(from.toLowerCase())}&destination=${encodeURIComponent(to.toLowerCase())}`);
      const list = Array.isArray(data) ? data : (Array.isArray(data?.trains) ? data.trains : []);
      const mapped = list.map(train => {
        const stations = train.stations || [];
        const times = train.stationTimes || {};
        const sourceKey = from.toLowerCase();
        const destinationKey = to.toLowerCase();
        const depart = times[sourceKey] || times[stations[0]] || '00:00:00';
        const arrive = times[destinationKey] || times[stations[stations.length - 1]] || depart;
        const matrix = Array.isArray(train.seats) ? train.seats : [];
        const freeCount = matrix.reduce((sum, row) => sum + (Array.isArray(row) ? row.filter(seat => Number(seat) === 0).length : 0), 0);
        return {
          id: train.trainId, trainId: train.trainId, number: train.trainNo || train.trainId,
          name: `Train ${train.trainNo || train.trainId}`, from: from, to: to,
          depart: String(depart).slice(0, 5), arrive: String(arrive).slice(0, 5),
          duration: formatDuration(String(depart), String(arrive)), seats: freeCount,
          fare: calculateSampleFare(stations, from, to), stops: stations.map(s => String(s).charAt(0).toUpperCase() + String(s).slice(1)),
          seatMatrix: matrix,
        };
      });
      setTrains(mapped);
      setNotice(mapped.length ? `Found ${mapped.length} train(s) from the backend.` : 'No trains found for this route in the backend data.');
    } catch (error) {
      setNotice(`Could not connect to the train API. Ensure Spring Boot is running on port 8080 and CORS allows the exact frontend origin (for example, localhost:5174). ${error.message}`);
    } finally {
      setLoadingTrains(false);
    }
    window.setTimeout(() => document.getElementById('results')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 60);
  }


  async function loadBookings() {
    if (!sessionStorage.getItem(SESSION_TOKEN_KEY) || !currentUser) {
      setAuthMode('login');
      setNotice('Please log in to view your bookings.');
      return;
    }

    setBookingsOpen(true);
    setBookingsLoading(true);
    setNotice('');
    try {
      const data = await apiRequest('/bookings/user');
      setBookings(Array.isArray(data) ? data : (Array.isArray(data?.bookings) ? data.bookings : []));
    } catch (error) {
      setNotice(`Could not load bookings: ${error.message}`);
      setBookings([]);
      if (error.message.toLowerCase().includes('token') || error.message.includes('401')) {
        signOut(false);
      }
    } finally {
      setBookingsLoading(false);
    }
  }

  async function cancelTicket(ticket) {
    if (!window.confirm(`Cancel ticket ${ticket.ticketId}?`)) return;
    try {
      await apiRequest(`/bookings/${encodeURIComponent(ticket.ticketId)}`, { method: 'DELETE' });
      setBookings(previous => previous.filter(item => item.ticketId !== ticket.ticketId));
      setNotice('Ticket cancelled successfully.');
    } catch (error) {
      setNotice(`Cancellation failed: ${error.message}`);
    }
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();
    setAuthBusy(true);
    setNotice('');
    try {
      if (authMode === 'signup') {
        await apiRequest('/auth/signup', {
          method: 'POST',
          body: JSON.stringify({
            name: authForm.name.trim(),
            email: authForm.email.trim(),
            password: authForm.password,
          }),
        });
        setAuthMode('login');
        setAuthForm(previous => ({ ...previous, password: '' }));
        setNotice('Account created successfully. Please log in.');
      } else {
        const data = await apiRequest('/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            email: authForm.email.trim(),
            password: authForm.password,
          }),
        });
        if (!data?.token || !data?.userId) {
          throw new Error('The server did not return an authentication token.');
        }
        const user = { userId: data.userId, name: data.name, email: data.email };
        sessionStorage.setItem(SESSION_TOKEN_KEY, data.token);
        sessionStorage.setItem(SESSION_USER_KEY, JSON.stringify(user));
        setCurrentUser(user);
        setAuthMode('');
        setAuthForm(previous => ({ ...previous, password: '' }));
        setNotice(`Welcome back, ${user.name || user.email}!`);
      }
    } catch (error) {
      setNotice(`${authMode === 'signup' ? 'Signup' : 'Login'} failed: ${error.message}`);
    } finally {
      setAuthBusy(false);
    }
  }

  async function signOut(showNotice = true) {
    const token = sessionStorage.getItem(SESSION_TOKEN_KEY);
    try {
      if (token) {
        await apiRequest('/auth/logout', { method: 'POST' });
      }
    } catch {
      // Clear local session even if the server session has already expired.
    } finally {
      sessionStorage.removeItem(SESSION_TOKEN_KEY);
      sessionStorage.removeItem(SESSION_USER_KEY);
      setCurrentUser(null);
      setBookings([]);
      setBookingsOpen(false);
      setAuthMode('');
      if (showNotice) setNotice('You have been logged out.');
    }
  }

  function swapStations() {
    setFrom(to);
    setTo(from);
  }

  async function handleBook(train) {
    if (!sessionStorage.getItem(SESSION_TOKEN_KEY) || !currentUser) {
      setAuthMode('login');
      setNotice('Please log in before booking a seat.');
      return;
    }
    setSelectedTrain(train);
    setSelectedSeats([]);
    setPassengers([{ name: '', age: '' }]);
    setBookingStep('seats');
    setNotice('Loading live seat availability...');
    try {
      const data = await apiRequest(`/trains/${encodeURIComponent(train.trainId)}/seats`);
      const matrix = Array.isArray(data) ? data : (Array.isArray(data?.seats) ? data.seats : (Array.isArray(data?.seatMatrix) ? data.seatMatrix : train.seatMatrix));
      setSeatMatrix(matrix || []);
      const available = (matrix || []).reduce((sum, row) => sum + (Array.isArray(row) ? row.filter(seat => Number(seat) === 0).length : 0), 0);
      setTrains(previous => previous.map(item => item.trainId === train.trainId ? { ...item, seats: available, seatMatrix: matrix || [] } : item));
      setNotice('');
    } catch (error) {
      setSeatMatrix(train.seatMatrix || []);
      setNotice(`Could not refresh seats. Showing the latest search result. ${error.message}`);
    }
  }

  function getSeats(train) {
    const matrix = selectedTrain?.trainId === train?.trainId ? seatMatrix : (train?.seatMatrix || []);
    return matrix.flatMap((row, rowIndex) => (Array.isArray(row) ? row : []).map((value, colIndex) => ({
      number: rowIndex * 7 + colIndex + 1, row: rowIndex, seat: colIndex, occupied: Number(value) !== 0,
    })));
  }

  function toggleSeat(seatNumber) {
    if (!selectedTrain) return;
    const seat = getSeats(selectedTrain).find(item => item.number === seatNumber);
    if (!seat || seat.occupied) return;
    setSelectedSeats(previous => {
      if (previous.includes(seatNumber)) return previous.filter(number => number !== seatNumber);
      if (previous.length >= 6) { setNotice('You can select a maximum of 6 seats.'); return previous; }
      setNotice('');
      return [...previous, seatNumber];
    });
  }

  // Update passenger information.
  function updatePassenger(index, field, value) {
    setPassengers(previous =>
      previous.map((passenger, passengerIndex) =>
        passengerIndex === index
          ? { ...passenger, [field]: value }
          : passenger
      )
    );
  }

  // Move to passenger details.
  function continueToPassengers() {
    if (selectedSeats.length === 0) {
      setNotice('Please select at least one seat.');
      return;
    }

    setPassengers(
      selectedSeats.map(() => ({
        name: '',
        age: ''
      }))
    );

    setNotice('');
    setBookingStep('passengers');
  }

  // Return to seat selection.
  function backToSeats() {
    setNotice('');
    setBookingStep('seats');
  }

  // Validate passenger details and show the booking summary.
  function reviewBooking(event) {
    event.preventDefault();

    const invalidPassenger = passengers.some(
      passenger =>
        !passenger.name.trim() ||
        !passenger.age ||
        Number(passenger.age) < 1 ||
        Number(passenger.age) > 120
    );

    if (invalidPassenger) {
      setNotice(
        'Enter a valid name and age for every passenger.'
      );
      return;
    }

    setNotice('');
    setBookingStep('summary');
  }

  return (
    <div className="app-shell">

      {/* Top information strip */}
      <div className="top-strip">
        <div className="container strip-inner">
          <span>
            <span className="live-dot" />
            Your journey, made simpler
          </span>

          <div className="strip-right">
            <span>Safe journeys start here</span>
            <span className="strip-separator">•</span>
            <span>Support: 24 × 7</span>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <header className="site-header">
        <div className="container nav-wrap">
          <a
            className="brand"
            href="#home"
            aria-label="RailwayGo home"
          >
            <span className="brand-mark">
              <Icon name="train" size={25} />
            </span>

            <span className="brand-name">
              railway<span>go</span>
              <small>TRAIN TICKETS, MADE EASY</small>
            </span>
          </a>

          <nav
            className="desktop-nav"
            aria-label="Main navigation"
          >
            <a className="active" href="#home">Home</a>

            <a
              href="#results"
              onClick={() => setSearched(true)}
            >
              Book tickets
            </a>

            <a href="#why-us">Why RailwayGo</a>
            <a href="#help">Help</a>
          </nav>

          <div className="nav-actions">
            <button className="nav-login" onClick={loadBookings}>My bookings</button>
            {currentUser ? (
              <>
                <span className="nav-login" aria-label="Signed in user">
                  <Icon name="user" size={17} />
                  {currentUser.name || currentUser.email}
                </span>
                <button className="nav-signup" onClick={() => signOut()}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <button
                  className="nav-login"
                  onClick={() => { setNotice(''); setAuthForm({ name: '', email: '', password: '' }); setAuthMode('login'); }}
                >
                  <Icon name="user" size={17} />
                  Log in
                </button>
                <button
                  className="nav-signup"
                  onClick={() => { setNotice(''); setAuthForm({ name: '', email: '', password: '' }); setAuthMode('signup'); }}
                >
                  Create account
                  <Icon name="arrow" size={16} />
                </button>
              </>
            )}
          </div>
        </div>
      </header>

      <main id="home">

        {/* Hero section */}
        <section className="hero">
          <div
            className="hero-grid"
            aria-hidden="true"
          />

          <div className="container hero-content">
            <div className="hero-copy">
              <div className="eyebrow">
                <span className="eyebrow-line" />
                YOUR NEXT JOURNEY STARTS HERE
              </div>

              <h1>
                Good journeys
                <br />
                begin with <span>one click.</span>
              </h1>

              <p>
                Find your train, choose your seat, and get ready
                to go. A simpler way to plan your next trip.
              </p>

              <div className="hero-points">
                <span>
                  <Icon name="check" size={16} />
                  Easy booking
                </span>

                <span>
                  <Icon name="check" size={16} />
                  Clear journey details
                </span>

                <span>
                  <Icon name="check" size={16} />
                  Travel with confidence
                </span>
              </div>
            </div>

            <div className="hero-art" aria-hidden="true">
              <div className="sun-glow" />
              <div className="route-line route-line-one" />
              <div className="route-line route-line-two" />

              <div className="floating-station station-one">
                <span className="station-dot" />
                <span>BANGALORE</span>
              </div>

              <div className="floating-station station-two">
                <span className="station-dot" />
                <span>YOUR NEXT STOP</span>
              </div>

              <div className="train-illustration">
                <div className="train-window-row">
                  <i />
                  <i />
                  <i />
                  <i />
                </div>

                <div className="train-front">
                  <span />
                  <span />
                </div>

                <div className="train-lower">
                  <i />
                  <i />
                  <i />
                  <i />
                </div>

                <div className="train-wheel wheel-one" />
                <div className="train-wheel wheel-two" />
                <div className="train-wheel wheel-three" />
              </div>

              <div className="art-caption">
                <span className="caption-icon">
                  <Icon name="pin" size={18} />
                </span>

                <span>
                  <b>Every journey matters</b>
                  <small>Make the most of the way</small>
                </span>
              </div>
            </div>
          </div>

          <div className="hero-bottom-wave" />
        </section>

        {/* Train search */}
        <section
          className="booking-section"
          aria-label="Search trains"
        >
          <div className="container booking-container">
            <div className="booking-card">
              <div className="booking-card-heading">
                <div>
                  <span className="section-kicker">
                    PLAN YOUR TRIP
                  </span>

                  <h2>Where would you like to go?</h2>
                </div>

                <div className="booking-label">
                  <Icon name="ticket" size={17} />
                  Train ticket booking
                </div>
              </div>

              <form
                className="search-form"
                onSubmit={searchTrains}
              >
                <div className="field station-field">
                  <label htmlFor="from">
                    <Icon name="pin" size={16} />
                    FROM
                  </label>

                  <select
                    id="from"
                    value={from}
                    onChange={event =>
                      setFrom(event.target.value)
                    }
                  >
                    {cityOptions.map(city => (
                      <option key={city}>{city}</option>
                    ))}
                  </select>

                  <span className="field-sub">
                    Departure station
                  </span>
                </div>

                <button
                  type="button"
                  className="swap-button"
                  onClick={swapStations}
                  aria-label="Swap departure and destination"
                >
                  <Icon name="swap" size={19} />
                </button>

                <div className="field station-field">
                  <label htmlFor="to">
                    <Icon name="pin" size={16} />
                    TO
                  </label>

                  <select
                    id="to"
                    value={to}
                    onChange={event =>
                      setTo(event.target.value)
                    }
                  >
                    {cityOptions.map(city => (
                      <option key={city}>{city}</option>
                    ))}
                  </select>

                  <span className="field-sub">
                    Arrival station
                  </span>
                </div>

                <div className="field date-field">
                  <label htmlFor="journey-date">
                    <Icon name="calendar" size={16} />
                    JOURNEY DATE
                  </label>

                  <input
                    id="journey-date"
                    type="date"
                    min={today}
                    value={date}
                    onChange={event =>
                      setDate(event.target.value)
                    }
                    required
                  />

                  <span className="field-sub">
                    Pick your travel day
                  </span>
                </div>

                <button
                  className="search-button"
                  type="submit"
                >
                  <Icon name="search" size={19} />
                  {loadingTrains ? 'Searching...' : 'Search trains'}
                </button>
              </form>

              <div className="booking-foot">
                <span>
                  <Icon name="shield" size={15} />
                  Your journey details stay private
                </span>

                <span>
                  Simple planning. Better journeys.
                </span>
              </div>
            </div>
          </div>
        </section>

        {/* Benefits */}
        <section className="trust-row">
          <div className="container trust-grid">
            <div className="trust-item">
              <span className="trust-icon blue">
                <Icon name="search" size={21} />
              </span>

              <div>
                <b>Easy train search</b>
                <p>Find a route in seconds</p>
              </div>
            </div>

            <div className="trust-item">
              <span className="trust-icon green">
                <Icon name="ticket" size={21} />
              </span>

              <div>
                <b>Clear booking details</b>
                <p>Know what you are booking</p>
              </div>
            </div>

            <div className="trust-item">
              <span className="trust-icon orange">
                <Icon name="clock" size={21} />
              </span>

              <div>
                <b>Plan with confidence</b>
                <p>Keep your trip organised</p>
              </div>
            </div>

            <div className="trust-item">
              <span className="trust-icon purple">
                <Icon name="shield" size={21} />
              </span>

              <div>
                <b>Designed for you</b>
                <p>A smooth booking experience</p>
              </div>
            </div>
          </div>
        </section>

        {/* Search results */}
        {searched && (
          <section className="results-section" id="results">
            <div className="container">
              <div className="results-heading">
                <div>
                  <span className="section-kicker">
                    YOUR ROUTE
                  </span>

                  <h2>Available trains</h2>

                  <p className="results-subtitle">
                    {from}
                    {' '}
                    <Icon name="arrow" size={15} />
                    {' '}
                    {to}
                    <span> · </span>

                    {new Date(
                      `${date}T00:00:00`
                    ).toLocaleDateString('en-IN', {
                      day: 'numeric',
                      month: 'short',
                      year: 'numeric'
                    })}
                  </p>
                </div>

                <label className="sort-control">
                  Sort by

                  <select
                    value={sort}
                    onChange={event =>
                      setSort(event.target.value)
                    }
                  >
                    <option value="recommended">
                      Recommended
                    </option>

                    <option value="fare">
                      Lowest fare
                    </option>

                    <option value="departure">
                      Departure time
                    </option>

                    <option value="duration">
                      Shortest journey
                    </option>
                  </select>
                </label>
              </div>

              {notice && (
                <div className="notice">
                  <span>{notice}</span>

                  <button
                    onClick={() => setNotice('')}
                    aria-label="Dismiss notice"
                  >
                    ×
                  </button>
                </div>
              )}

              <div className="results-toolbar">
                <span className="results-count">
                  <b>{results.length}</b>
                  {' '}
                  train{results.length === 1 ? '' : 's'} found
                </span>

                <div className="result-filters">
                  <label>
                    Departure

                    <select
                      value={departureFilter}
                      onChange={event =>
                        setDepartureFilter(event.target.value)
                      }
                    >
                      <option value="any">Any time</option>
                      <option value="morning">
                        Morning · 5 AM–12 PM
                      </option>
                      <option value="afternoon">
                        Afternoon · 12–5 PM
                      </option>
                      <option value="evening">
                        Evening · 5–9 PM
                      </option>
                      <option value="night">
                        Night · 9 PM–5 AM
                      </option>
                    </select>
                  </label>

                  <label className="available-toggle">
                    <input
                      type="checkbox"
                      checked={availableOnly}
                      onChange={event =>
                        setAvailableOnly(event.target.checked)
                      }
                    />

                    Seats available only
                  </label>
                </div>
              </div>

              {results.length ? (
                <div className="train-list">
                  {results.map(train => (
                    <article
                      className="train-card"
                      key={train.id}
                    >
                      <div className="train-main">
                        <div className="train-title">
                          <span className="train-icon">
                            <Icon name="train" size={20} />
                          </span>

                          <div>
                            <h3>{train.name}</h3>
                            <p>Train no. {train.number}</p>
                          </div>
                        </div>

                        <div className="train-timing">
                          <div>
                            <b>{train.depart}</b>
                            <span>{train.from}</span>
                          </div>

                          <div className="journey-duration">
                            <span>{train.duration}</span>

                            <div>
                              <i />
                              <span />
                              <i />
                            </div>

                            <small>
                              {Math.max(
                                0,
                                train.stops.length - 2
                              )}
                              {' '}
                              intermediate stop
                              {train.stops.length - 2 === 1
                                ? ''
                                : 's'}
                            </small>
                          </div>

                          <div>
                            <b>{train.arrive}</b>
                            <span>{train.to}</span>
                          </div>
                        </div>
                      </div>

                      <div className="train-side">
                        <div
                          className={`seat-info ${
                            train.seats === 0 ? 'sold-out' : ''
                          }`}
                        >
                          <span className="seat-dot" />

                          <b>
                            {train.seats === 0
                              ? 'Waitlist only'
                              : `${train.seats} seats`}
                          </b>

                          <small>
                            {train.seats === 0
                              ? 'currently unavailable'
                              : 'available'}
                          </small>
                        </div>

                        <div className="fare">
                          {train.fare == null ? 'Unavailable' : formatRupees(train.fare)}
                          <small>sample fare per passenger</small>
                        </div>

                        <button
                          className="book-button"
                          onClick={() => handleBook(train)}
                        >
                          {train.seats === 0
                            ? 'View details'
                            : 'View & book'}

                          <Icon name="arrow" size={16} />
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="empty-state">
                  <div className="empty-icon">
                    <Icon name="train" size={30} />
                  </div>

                  <h3>No matching trains found</h3>

                  <p>
                    Try changing your route or relaxing the filters.
                  </p>

                  <button
                    className="outline-button"
                    onClick={() => {
                      setFrom('Bangalore');
                      setTo('Delhi');
                      setSort('recommended');
                      setDepartureFilter('any');
                      setAvailableOnly(false);
                    }}
                  >
                    Try Bangalore to Delhi
                  </button>
                </div>
              )}
</div>
          </section>
        )}

        {/* Why RailwayGo */}
        <section className="why-section" id="why-us">
          <div className="container why-layout">
            <div className="why-copy">
              <span className="section-kicker">
                TRAVEL, WITHOUT THE FUSS
              </span>

              <h2>
                Less time figuring it out.
                <br />
                <span>More time looking forward.</span>
              </h2>

              <p>
                Trip planning should feel straightforward.
                RailwayGo brings the essentials together so
                you can focus on the journey ahead.
              </p>

              <a className="text-link" href="#home">
                Plan your next trip
                <Icon name="arrow" size={17} />
              </a>
            </div>

            <div className="why-card-grid">
              <div className="why-card">
                <span className="why-number">01</span>

                <div className="why-card-icon">
                  <Icon name="search" size={21} />
                </div>

                <h3>Find your route</h3>

                <p>
                  Start with your stations and travel date.
                  Keep the essentials in one place.
                </p>
              </div>

              <div className="why-card">
                <span className="why-number">02</span>

                <div className="why-card-icon">
                  <Icon name="ticket" size={21} />
                </div>

                <h3>Review your options</h3>

                <p>
                  Compare journey times, fares, and seat
                  availability before choosing.
                </p>
              </div>

              <div className="why-card">
                <span className="why-number">03</span>

                <div className="why-card-icon">
                  <Icon name="check" size={21} />
                </div>

                <h3>Travel prepared</h3>

                <p>
                  Keep booking details organised for a
                  smoother travel day.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Call to action */}
        <section className="cta-section" id="help">
          <div className="container cta-box">
            <div>
              <span className="section-kicker">
                READY WHEN YOU ARE
              </span>

              <h2>Your next destination is waiting.</h2>

              <p>
                Start with a route. We'll help you take it from there.
              </p>
            </div>

            <a href="#home" className="cta-button">
              Search a journey
              <Icon name="arrow" size={17} />
            </a>

            <div className="cta-decoration" aria-hidden="true">
              <Icon name="train" size={90} />
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="site-footer">
        <div className="container footer-main">
          <a className="brand footer-brand" href="#home">
            <span className="brand-mark">
              <Icon name="train" size={23} />
            </span>

            <span className="brand-name">
              railway<span>go</span>
              <small>TRAIN TICKETS, MADE EASY</small>
            </span>
          </a>

          <p>A clearer way to plan your next train journey.</p>

          <div className="footer-links">
            <a href="#home">Home</a>

            <a
              href="#results"
              onClick={() => setSearched(true)}
            >
              Book tickets
            </a>

            <a href="#why-us">About</a>
            <a href="#help">Help</a>
          </div>
        </div>

        <div className="container footer-bottom">
          <span>
            © {new Date().getFullYear()} RailwayGo. Project preview.
          </span>

          <span>
            Built for simpler journeys
            {' '}
            <span className="footer-heart">♥</span>
          </span>
        </div>
      </footer>

      {/* =====================================
          SEAT SELECTION AND BOOKING MODAL
         ===================================== */}

      {selectedTrain && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={event => {
            if (event.target === event.currentTarget) {
              setSelectedTrain(null);
            }
          }}
        >
          <section
            className="modal-card seat-booking-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="booking-modal-title"
          >
            <button
              className="modal-close"
              onClick={() => setSelectedTrain(null)}
              aria-label="Close booking"
            >
              ×
            </button>

            <span className="modal-icon">
              <Icon name="train" size={24} />
            </span>

            <span className="section-kicker">
              {bookingStep === 'seats'
                ? 'STEP 1 OF 3'
                : bookingStep === 'passengers'
                  ? 'STEP 2 OF 3'
                  : 'STEP 3 OF 3'}
            </span>

            <h2 id="booking-modal-title">
              {bookingStep === 'seats'
                ? 'Choose your seats'
                : bookingStep === 'passengers'
                  ? 'Passenger details'
                  : 'Review your booking'}
            </h2>

            <p className="modal-intro">
              {selectedTrain.name}
              {' · '}
              Train {selectedTrain.number}
            </p>

            {/* Journey information */}
            <div className="seat-route-summary">
              <div>
                <small>FROM</small>
                <strong>{selectedTrain.from}</strong>
                <span>{selectedTrain.depart}</span>
              </div>

              <Icon name="arrow" size={20} />

              <div>
                <small>TO</small>
                <strong>{selectedTrain.to}</strong>
                <span>{selectedTrain.arrive}</span>
              </div>
            </div>

            {/* Step 1: Seat selection */}
            {bookingStep === 'seats' && (
              <>
                <div className="seat-date">
                  <Icon name="calendar" size={17} />

                  {new Date(`${date}T00:00:00`).toLocaleDateString(
                    'en-IN',
                    {
                      day: 'numeric',
                      month: 'long',
                      year: 'numeric'
                    }
                  )}
                </div>

                <div className="seat-legend">
                  <span>
                    <i className="legend-seat available-seat" />
                    Available
                  </span>

                  <span>
                    <i className="legend-seat selected-seat" />
                    Selected
                  </span>

                  <span>
                    <i className="legend-seat occupied-seat" />
                    Occupied
                  </span>
                </div>

                <div className="coach-label">
                  <span>COACH PREVIEW</span>
                  <span>{getSeats(selectedTrain).length} seats</span>
                </div>

                <div className="seat-coach">
                  <div className="coach-front">
                    <Icon name="train" size={20} />
                    <span>Coach entrance</span>
                  </div>

                  <div className="seat-grid">
                    {getSeats(selectedTrain).map(seat => {
                      const isSelected = selectedSeats.includes(
                        seat.number
                      );

                      return (
                        <button
                          key={seat.number}
                          type="button"
                          className={[
                            'seat',
                            seat.occupied
                              ? 'seat-occupied'
                              : 'seat-available',
                            isSelected ? 'seat-selected' : ''
                          ].join(' ')}
                          disabled={seat.occupied}
                          aria-pressed={isSelected}
                          aria-label={`Seat ${seat.number}${
                            seat.occupied
                              ? ', occupied'
                              : isSelected
                                ? ', selected'
                                : ', available'
                          }`}
                          onClick={() => toggleSeat(seat.number)}
                        >
                          {seat.number}
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="seat-selection-summary">
                  <div>
                    <span>Selected seats</span>

                    <strong>
                      {selectedSeats.length
                        ? selectedSeats.join(', ')
                        : 'None selected'}
                    </strong>
                  </div>

                  <div>
                    <span>Fare per passenger</span>
                    <strong>{selectedTrain.fare == null ? 'Unavailable' : formatRupees(selectedTrain.fare)}</strong>
                  </div>

                  <div>
                    <span>Total fare</span>

                    <strong>
                      {selectedTrain.fare == null ? 'Unavailable' : formatRupees(selectedTrain.fare * selectedSeats.length)}
                    </strong>
                  </div>
                </div>

                {notice && (
                  <div className="booking-notice" role="alert">
                    {notice}
                  </div>
                )}

                <button
                  className="search-button modal-confirm"
                  onClick={continueToPassengers}
                >
                  Continue to passengers
                  <Icon name="arrow" size={17} />
                </button>
              </>
            )}

            {/* Step 2: Passenger details */}
            {bookingStep === 'passengers' && (
              <form onSubmit={reviewBooking}>
                <div className="passenger-heading">
                  <h3>Passenger information</h3>

                  <span>
                    {selectedSeats.length}
                    {' '}
                    {selectedSeats.length === 1
                      ? 'passenger'
                      : 'passengers'}
                  </span>
                </div>

                {passengers.map((passenger, index) => (
                  <div
                    className="passenger-form-card"
                    key={index}
                  >
                    <div className="passenger-card-heading">
                      <strong>
                        Passenger {index + 1}
                      </strong>

                      <span>
                        Seat {selectedSeats[index]}
                      </span>
                    </div>

                    <label className="auth-label">
                      Full name

                      <input
                        type="text"
                        value={passenger.name}
                        placeholder="Enter passenger name"
                        maxLength={100}
                        required
                        onChange={event =>
                          updatePassenger(
                            index,
                            'name',
                            event.target.value
                          )
                        }
                      />
                    </label>

                    <label className="auth-label">
                      Age

                      <input
                        type="number"
                        value={passenger.age}
                        placeholder="Enter age"
                        min="1"
                        max="120"
                        required
                        onChange={event =>
                          updatePassenger(
                            index,
                            'age',
                            event.target.value
                          )
                        }
                      />
                    </label>
                  </div>
                ))}

                {notice && (
                  <div className="booking-notice" role="alert">
                    {notice}
                  </div>
                )}

                <div className="booking-button-row">
                  <button
                    type="button"
                    className="outline-button"
                    onClick={backToSeats}
                  >
                    Back
                  </button>

                  <button
                    type="submit"
                    className="search-button"
                  >
                    Review booking
                    <Icon name="arrow" size={17} />
                  </button>
                </div>
              </form>
            )}

            {/* Step 3: Booking summary */}
            {bookingStep === 'summary' && (
              <>
                <div className="booking-success-icon">
                  <Icon name="check" size={30} />
                </div>

                <h3 className="summary-heading">
                  Your booking summary
                </h3>

                <p className="modal-intro">
                  Review the details before connecting to the
                  booking service.
                </p>

                <div className="booking-summary-details">
                  <div>
                    <span>Train</span>

                    <strong>
                      {selectedTrain.name}
                      {' ('}
                      {selectedTrain.number}
                      {')'}
                    </strong>
                  </div>

                  <div>
                    <span>Journey date</span>

                    <strong>
                      {new Date(`${date}T00:00:00`).toLocaleDateString(
                        'en-IN',
                        {
                          day: 'numeric',
                          month: 'long',
                          year: 'numeric'
                        }
                      )}
                    </strong>
                  </div>

                  <div>
                    <span>Route</span>

                    <strong>
                      {selectedTrain.from}
                      {' → '}
                      {selectedTrain.to}
                    </strong>
                  </div>

                  <div>
                    <span>Departure</span>
                    <strong>{selectedTrain.depart}</strong>
                  </div>

                  <div>
                    <span>Arrival</span>
                    <strong>{selectedTrain.arrive}</strong>
                  </div>

                  <div>
                    <span>Selected seats</span>
                    <strong>{selectedSeats.join(', ')}</strong>
                  </div>

                  <div>
                    <span>Passengers</span>
                    <strong>{passengers.length}</strong>
                  </div>

                  {passengers.map((passenger, index) => (
                    <div key={index}>
                      <span>Passenger {index + 1}</span>

                      <strong>
                        {passenger.name}, age {passenger.age}
                      </strong>
                    </div>
                  ))}

                  <div className="summary-total">
                    <span>Total fare</span>

                    <strong>
                      {selectedTrain.fare == null ? 'Unavailable' : formatRupees(selectedTrain.fare * selectedSeats.length)}
                    </strong>
                  </div>
                </div>

                <div className="booking-notice">
                  Seat availability is loaded from the backend. Sample fare: ₹150 per route segment. This is for the project demo and is not an official Indian Railways fare.
                </div>

                <div className="booking-button-row">
                  <button
                    className="outline-button"
                    onClick={() => setBookingStep('passengers')}
                  >
                    Edit passengers
                  </button>

                  <button
                    className="search-button"
                    disabled={bookingBusy}
                    onClick={async () => {
                      if (!selectedTrain || !selectedSeats.length) return;
                      setBookingBusy(true);
                      setNotice('Booking your selected seats...');
                      try {
                        const booked = [];
                        for (const number of selectedSeats) {
                          const row = Math.floor((number - 1) / 7);
                          const seat = (number - 1) % 7;
                          const result = await apiRequest('/bookings', {
                            method: 'POST',
                            body: JSON.stringify({
                              trainId: selectedTrain.trainId, row, seat,
                              source: from, destination: to, travelDate: date,
                            }),
                          });
                          booked.push(result);
                        }
                        let refreshedMatrix = null;
                        try {
                          const latest = await apiRequest(`/trains/${encodeURIComponent(selectedTrain.trainId)}/seats`);
                          refreshedMatrix = Array.isArray(latest) ? latest : (Array.isArray(latest?.seats) ? latest.seats : (Array.isArray(latest?.seatMatrix) ? latest.seatMatrix : null));
                        } catch { /* Booking succeeded; retain a safe local count fallback if refresh fails. */ }
                        const refreshedAvailable = Array.isArray(refreshedMatrix)
                          ? refreshedMatrix.reduce((sum, row) => sum + (Array.isArray(row) ? row.filter(value => Number(value) === 0).length : 0), 0)
                          : null;
                        setTrains(previous => previous.map(item => item.trainId === selectedTrain.trainId
                          ? { ...item, seats: refreshedAvailable ?? Math.max(0, item.seats - booked.length), ...(refreshedMatrix ? { seatMatrix: refreshedMatrix } : {}) }
                          : item));
                        setSelectedTrain(null);
                        setSeatMatrix([]);
                        const totalFare = booked.reduce((sum, ticket) => sum + Number(ticket.fare || selectedTrain.fare || 0), 0);
                        setNotice(`Booking successful! ${booked.length} seat(s) booked for ${currentUser?.name || currentUser?.email || 'your account'}. Total fare: ${formatRupees(totalFare)}.`);
                      } catch (error) {
                        setNotice(`Booking request failed: ${error.message}. If some seats were booked before the error, check booking history before retrying.`);
                      } finally {
                        setBookingBusy(false);
                      }
                    }}
                  >
                    {bookingBusy ? 'Booking...' : 'Confirm booking'}
                    <Icon name="check" size={17} />
                  </button>
                </div>
              </>
            )}

            <p className="modal-footnote">
              Seat availability and booking requests are connected to the Java backend. Passenger names/ages are collected in this UI but are not currently stored by the backend API.
            </p>
          </section>
        </div>
      )}


      {bookingsOpen && (
        <div className="modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setBookingsOpen(false); }}>
          <section className="modal-card bookings-modal" role="dialog" aria-modal="true" aria-labelledby="bookings-title">
            <button className="modal-close" onClick={() => setBookingsOpen(false)} aria-label="Close bookings">×</button>
            <span className="modal-icon"><Icon name="ticket" size={24} /></span>
            <span className="section-kicker">YOUR TRAVEL</span>
            <h2 id="bookings-title">My bookings</h2>
            <p className="modal-intro">Tickets saved to your signed-in account.</p>
            {bookingsLoading ? <p>Loading bookings…</p> : bookings.length === 0 ? <p>No bookings found for this account.</p> : (
              <div className="booking-list">
                {bookings.map(ticket => {
                  const train = ticket.train || {};
                  const stations = train.stations || [];
                  const seatLabel = Number(ticket.seatRow) * 7 + Number(ticket.seatNumber) + 1;
                  return <article className="booking-list-item" key={ticket.ticketId}>
                    <div className="booking-list-head"><strong>Train {train.trainNo || train.trainId || '—'}</strong><span>Seat {seatLabel}</span></div>
                    <p>{String(ticket.source || stations[0] || '—').toUpperCase()} → {String(ticket.destination || stations[stations.length - 1] || '—').toUpperCase()}</p>
                    <div className="booking-list-meta"><span>Date: {ticket.dateOfTravel || '—'}</span><span>Fare: {ticket.fare == null || Number(ticket.fare) === 0 ? 'Not available for this ticket' : formatRupees(ticket.fare)}</span><span>Ticket ID: {ticket.ticketId}</span></div>
                    <button className="outline-button cancel-ticket" onClick={() => cancelTicket(ticket)}>Cancel ticket</button>
                  </article>;
                })}
              </div>
            )}
            {notice && <div className="booking-notice" role="status">{notice}</div>}
            <button className="search-button modal-confirm" onClick={loadBookings}>Refresh bookings</button>
            <p className="modal-footnote">Your booking history is loaded from the backend using your authentication token.</p>
          </section>
        </div>
      )}

      {/* Login and signup */}
      {authMode && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={event => {
            if (event.target === event.currentTarget) {
              setAuthMode('');
            }
          }}
        >
          <section
            className="modal-card auth-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="auth-title"
          >
            <button
              className="modal-close"
              onClick={() => setAuthMode('')}
              aria-label="Close"
            >
              ×
            </button>

            <span className="modal-icon">
              <Icon name="user" size={24} />
            </span>

            <span className="section-kicker">
              WELCOME TO RAILWAYGO
            </span>

            <h2 id="auth-title">
              {authMode === 'login'
                ? 'Welcome back'
                : 'Create your account'}
            </h2>

            <p className="modal-intro">
              {authMode === 'login'
                ? 'Log in to manage your journeys.'
                : 'A few details to get your journeys started.'}
            </p>

            <form onSubmit={handleAuthSubmit}>
              {authMode === 'signup' && (
                <label className="auth-label">
                  Full name
                  <input
                    type="text"
                    placeholder="Your name"
                    value={authForm.name}
                    maxLength={100}
                    autoComplete="name"
                    onChange={event => setAuthForm(previous => ({ ...previous, name: event.target.value }))}
                    required
                  />
                </label>
              )}

              <label className="auth-label">
                Email address
                <input
                  type="email"
                  placeholder="you@example.com"
                  value={authForm.email}
                  autoComplete="email"
                  onChange={event => setAuthForm(previous => ({ ...previous, email: event.target.value }))}
                  required
                />
              </label>

              <label className="auth-label">
                Password
                <input
                  type="password"
                  placeholder="Enter your password"
                  value={authForm.password}
                  minLength={8}
                  autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
                  onChange={event => setAuthForm(previous => ({ ...previous, password: event.target.value }))}
                  required
                />
              </label>

              {notice && <div className="booking-notice" role="alert">{notice}</div>}

              <button className="search-button modal-confirm" type="submit" disabled={authBusy}>
                {authBusy ? 'Please wait…' : authMode === 'login' ? 'Log in' : 'Create account'}
                <Icon name="arrow" size={17} />
              </button>
            </form>

            <p className="auth-switch">
              {authMode === 'login'
                ? 'New to RailwayGo?'
                : 'Already have an account?'}

              {' '}

              <button
                onClick={() =>
                  { setNotice(''); setAuthForm(previous => ({ ...previous, password: '' })); setAuthMode(authMode === 'login' ? 'signup' : 'login'); }
                }
              >
                {authMode === 'login'
                  ? 'Create account'
                  : 'Log in'}
              </button>
            </p>

            <p className="modal-footnote">
              Your account is connected to the RailwayGo backend. Keep your password and session private.
            </p>
          </section>
        </div>
      )}
    </div>
  );
}

export default App;
