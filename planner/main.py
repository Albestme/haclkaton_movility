import random
import math
import time
from collections import defaultdict
import csv
import requests

# =========================
# CONFIGURATION PARAMETERS
# =========================

WORK_START = 8 * 60  # 08:00 = 480 minutes

def default_params():
    return {
        "time_budget": 8 * 60,  # minutes
        "priority_weight": 10.0,
        "priority_penalization": 25.0,
        "travel_weight": 0.8,
        "aging_factor": 0.05,
        "exploration_factor": 1.1,
        "num_simulations": 10000,
        "num_simulations_local": 500,
        "radius": 60.0,  # distance threshold in minutes
        "freeze_window": 30,  # minutes
        "critical_threshold": 3,
        "undertime_penalty": 90,
        "overtime_penalty": 250,
        "grace_period": 20
    }

# =========================
# DATA MODELS (UPDATED)
# =========================

class Task:
    def __init__(self, id, lat, lon, priority, service_time, created_at=480, av=True):
        self.id = id
        self.lat = lat
        self.lon = lon
        self.priority = priority
        self.service_time = service_time
        self.created_at = created_at  # minutes of day
        self.available = av

class Worker:
    def __init__(self, id, lat, lon):
        self.id = id
        self.lat = lat
        self.lon = lon
        self.route = []

# =========================
# UTILS
# =========================

def haversine_distance(a, b):
    R = 6371
    lat1, lon1 = math.radians(a.lat), math.radians(a.lon)
    lat2, lon2 = math.radians(b.lat), math.radians(b.lon)

    dlat = lat2 - lat1
    dlon = lon2 - lon1

    h = math.sin(dlat/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dlon/2)**2
    return 2 * R * math.asin(math.sqrt(h))


_cache = {}
LAST_CALL = 0

def rate_limited_get(url, timeout=5):
    global LAST_CALL
    elapsed = time.time() - LAST_CALL
    if elapsed < 4.0:  # 4 req/sec
        time.sleep(1.0 - elapsed)

    LAST_CALL = time.time()
    return requests.get(url, timeout)

def osrm_route(a, b):
    key = (
        round(a.lat, 4), round(a.lon, 4),
        round(b.lat, 4), round(b.lon, 4)
    )

    if key in _cache:
        return _cache[key]

    url = f"http://router.project-osrm.org/route/v1/driving/{a.lon},{a.lat};{b.lon},{b.lat}?overview=false"

    try:
        response = requests.rate_limited_get(url, timeout=5)
        data = response.json()

        if data["code"] != "Ok":
            raise Exception()

        route = data["routes"][0]
        result = (route["distance"] / 1000, route["duration"] / 60)

    except:
        distance_km = haversine_distance(a, b)
        result = (distance_km, distance_km)

    _cache[key] = result
    return result

def mock_traffic_api(a, b, t_minutes):
    hour = (t_minutes // 60) % 24

    if 7 <= hour <= 9 or 17 <= hour <= 18:
        return 1.5
    elif 0 <= hour <= 5:
        return 0.7
    return 1.0


def travel_time(a, b, t):
    #_, base_time = osrm_route(a, b)

    traffic_factor = mock_traffic_api(a, b, t)
    base_time = haversine_distance(a, b)
    return base_time * traffic_factor


def effective_priority(task, current_time, params):
    waiting = current_time - task.created_at
    if task.priority >= params["critical_threshold"]:
        return task.priority
    return task.priority + params["aging_factor"] * waiting


def selection_score(task, travel, current_time, params):
    priority = effective_priority(task, current_time, params)
    noise = random.random() * params["exploration_factor"]
    return params["priority_weight"] * priority - params["travel_weight"] * travel + noise

# =========================
# MONTE CARLO SIMULATION
# =========================

def simulate_solution(workers, tasks, params):
    remaining = tasks[:]
    solution = {w.id: [] for w in workers}

    for w in workers:
        current = Task(-1, w.lat, w.lon, 0, 0)
        current_time = WORK_START
        time_left = params["time_budget"]

        while remaining and time_left > 0:
            candidates = []
            for task in remaining:
                #print("w=", w.id, " t=", task.id)
                #if task.available == True:
                    travel = travel_time(current, task, current_time)
                    score = selection_score(task, travel, current_time, params)
                    candidates.append((task, score, travel))

            candidates.sort(key=lambda x: -x[1])
            task, _, travel = candidates[0]

            total = travel + task.service_time
            if total > time_left:
                break

            solution[w.id].append(task)
            task.available = False
            remaining.remove(task)

            current = task
            current_time += total
            time_left -= total

    return solution


def evaluate(solution, tasks, params):
    score = 0
    pend_tasks = tasks.copy()
    pend_penalization = 0
    for route in solution.values():
        time_used = 0
        for task in route:
            score += params["priority_weight"] * task.priority
            time_used += task.service_time
            pend_tasks.remove(task)
        if time_used > params["time_budget"]:
            score -= params["overtime_penalty"]
        elif time_used < (params["time_budget"] - params["grace_period"]):
            score -= params["undertime_penalty"]
    for task in pend_tasks:
        pend_penalization += task.priority * params["priority_penalization"]
    return score - pend_penalization


def monte_carlo(workers, tasks, params, local=False):
    best = None
    best_score = -float("inf")

    sims = params["num_simulations_local"] if local else params["num_simulations"]

    for _ in range(sims):
        sol = simulate_solution(workers, tasks.copy(), params)
        sc = evaluate(sol, tasks, params)

        if sc > best_score:
            best_score = sc
            best = sol

    return best

# =========================
# RESCHEDULER
# =========================

def get_local_region(task, workers, params):
    nearby = []
    for w in workers:
        d = travel_time(w, task, task.created_at)
        if d < params["radius"]:
            nearby.append(w)
    return nearby


def reschedule(new_task, workers, all_tasks, params):
    local_workers = get_local_region(new_task, workers, params)

    local_tasks = []
    for w in local_workers:
        local_tasks.extend(w.route)
        w.route = []

    local_tasks.append(new_task)

    new_solution = monte_carlo(local_workers, local_tasks, params, local=True)

    for w in local_workers:
        w.route = new_solution[w.id]

# =========================
# TEST / SIMULATION
# =========================

def read_tasks_from_csv(filename, limit=100):
    tasks = []

    with open(filename, newline='') as csvfile:
        reader = csv.DictReader(csvfile)

        for i, row in enumerate(reader):
            if limit and i >= limit:
                break

            task = Task(
                id=int(row["id"]),
                lat=float(row["lat"]),
                lon=float(row["lon"]),
                priority=int(row["priority"]),
                service_time=float(row["service_time"]),
                created_at=480,
                av=True
            )

            tasks.append(task)
    return tasks

def generate_tasks(n):
    tasks = []
    for i in range(n):
        tasks.append(Task(
            i,
            random.uniform(40.9, 41.5),
            random.uniform(0.5, 1.65),
            random.randint(0, 4),
            service_time=random.uniform(60, 180),
            created_at=random.uniform(0, 24*60),  # between 08:00 and 20:00
            av=True
        ))
    return tasks


def generate_workers(n):
    workers = []
    for i in range(n):
        workers.append(Worker(
            i,
            random.uniform(40.9, 41.5),
            random.uniform(0.5, 1.65)
        ))
    return workers


def run_simulation():
    params = default_params()

    workers = generate_workers(4)
    tasks = generate_tasks(25)
    #tasks = read_tasks_from_csv("tasks.csv", limit=100)

    start = time.time()
    solution = monte_carlo(workers, tasks, params)
    end = time.time()

    print("Initial solution time: ", end - start, " | Score: ", evaluate(solution, tasks, params))

    for w in workers:
        w.route = solution[w.id]
        print("Worker ", w.id, ":")
        prev_task = Task(-1,w.lat,w.lon,6,0)
        local_time = 0
        for task in w.route:
            try:
                step = travel_time(prev_task, task, local_time)
                print(f"\t({local_time:.2f})- Trip from {prev_task.id if prev_task.id!=-1 else 'Start'} to {task.id} takes {step:.2f}")
                local_time += step
            except:
                pass
            print(f"\t({local_time:.2f})- Task {task.id} Duration: {task.service_time:.2f} Priority: {task.priority}")
            local_time += task.service_time
            prev_task = task
        try:
            step = travel_time(prev_task, Task(-1, w.lat, w.lon, 6, 0), local_time)
            print(f"\t({local_time:.2f})- Trip from {prev_task.id} to Depot takes {step:.2f}")
            local_time += step
        except:
            pass
        print(f"Final worked time: {(local_time/60):.2f}")
    
    print("Pending tasks:")
    for task in tasks:
        print(f"\t- Task {task.id} Duration: {task.service_time:.2f} Priority: {task.priority}")

    ## simulate new task
    #new_task = Task(999, 50, 50, 6, 30)
    #reschedule(new_task, workers, tasks, params)

    export_svg(workers, tasks)
    total_tasks = sum(len(w.route) for w in workers)
    print("Total assigned tasks after reschedule:", total_tasks)

# =========================
# SVG CHECK
# =========================

def export_svg(workers, tasks, filename="solution.svg"):
    width, height = 800, 800
    margin = 50

    # Collect all coordinates
    all_lats = [t.lat for t in tasks] + [w.lat for w in workers]
    all_lons = [t.lon for t in tasks] + [w.lon for w in workers]

    min_lat, max_lat = min(all_lats), max(all_lats)
    min_lon, max_lon = min(all_lons), max(all_lons)

    # Avoid division by zero
    lat_range = max_lat - min_lat or 1
    lon_range = max_lon - min_lon or 1

    def scale_x(lon):
        return margin + (lon - min_lon) / lon_range * (width - 2 * margin)

    def scale_y(lat):
        # invert Y axis because SVG origin is top-left
        return height - margin - (lat - min_lat) / lat_range * (height - 2 * margin)

    def task_color(priority):
        r = int(255 * (priority / 6))
        b = 255 - r
        return f"rgb({r},0,{b})"

    svg = []

    # Header
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}">')
    svg.append('<rect width="100%" height="100%" fill="white"/>')

    # Draw all tasks
    for t in tasks:
        x, y = scale_x(t.lon), scale_y(t.lat)
        color = task_color(t.priority)

        svg.append(f'<circle cx="{x}" cy="{y}" r="5" fill="{color}" />')
        svg.append(f'<text x="{x+6}" y="{y-6}" font-size="10">T{t.id}</text>')

    # Draw workers and routes
    colors = ["red", "green", "blue", "orange", "purple", "black"]

    for i, w in enumerate(workers):
        color = colors[i % len(colors)]

        start_x, start_y = scale_x(w.lon), scale_y(w.lat)

        # Start depot
        svg.append(f'<rect x="{start_x-6}" y="{start_y-6}" width="12" height="12" fill="{color}" />')
        svg.append(f'<text x="{start_x+8}" y="{start_y}" font-size="12">W{w.id} start</text>')

        prev_x, prev_y = start_x, start_y

        # Route
        for task in w.route:
            x, y = scale_x(task.lon), scale_y(task.lat)

            svg.append(f'<line x1="{prev_x}" y1="{prev_y}" x2="{x}" y2="{y}" stroke="{color}" stroke-width="2"/>')
            prev_x, prev_y = x, y

        # Return to depot
        svg.append(f'<line x1="{prev_x}" y1="{prev_y}" x2="{start_x}" y2="{start_y}" stroke="{color}" stroke-dasharray="4"/>')

        # End marker
        svg.append(f'<circle cx="{start_x}" cy="{start_y}" r="8" fill="none" stroke="{color}" stroke-width="2"/>')
        svg.append(f'<text x="{start_x+8}" y="{start_y+12}" font-size="12">W{w.id} end</text>')

    svg.append("</svg>")

    with open(filename, "w") as f:
        f.write("\n".join(svg))

    print(f"SVG exported to {filename}")

# =========================
# EVOLUTIONARY TUNER (OPTIONAL)
# =========================

def mutate(params):
    new = params.copy()
    for k in new:
        if isinstance(new[k], (int, float)):
            new[k] *= random.uniform(0.8, 1.2)
    return new


def evolutionary_tuning():
    population = [default_params() for _ in range(10)]

    for generation in range(10):
        scored = []

        for p in population:
            workers = generate_workers(3)
            tasks = generate_tasks(20)
            sol = monte_carlo(workers, tasks, p)
            score = evaluate(sol, p)
            scored.append((score, p))

        scored.sort(reverse=True, key=lambda x: x[0])

        population = [p for _, p in scored[:5]]

        while len(population) < 10:
            parent = random.choice(population)
            population.append(mutate(parent))

        print(f"Generation {generation} best score: {scored[0][0]}")

    return population[0]


if __name__ == "__main__":
    run_simulation()
    # best_params = evolutionary_tuning()
    # print("Best params:", best_params)
