import random
import math
import time
from collections import defaultdict

# =========================
# CONFIGURATION PARAMETERS
# =========================

def default_params():
    return {
        "time_budget": 6 * 60,  # minutes
        "priority_weight": 5.0,
        "travel_weight": 1.0,
        "aging_factor": 0.02,
        "exploration_factor": 1.0,
        "num_simulations": 2000,
        "num_simulations_local": 250,
        "radius": 30.0,  # distance threshold
        "freeze_window": 30,  # minutes
        "critical_threshold": 5,
        "overtime_penalty": 1000
    }

# =========================
# DATA MODELS
# =========================

class Task:
    def __init__(self, id, x, y, priority, service_time, created_at=0, av=True):
        self.id = id
        self.x = x
        self.y = y
        self.priority = priority
        self.service_time = service_time
        self.created_at = created_at
        self.available = av

class Worker:
    def __init__(self, id, x, y):
        self.id = id
        self.x = x
        self.y = y
        self.route = []

# =========================
# UTILS
# =========================

def distance(a, b):
    return math.hypot(a.x - b.x, a.y - b.y)


def travel_time(a, b, t):
    base = distance(a, b)
    traffic_factor = 1 + 0.5 * math.sin(t / 60.0)
    return base * traffic_factor


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
        current = Task(-1, w.x, w.y, 0, 0)
        current_time = 0
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


def evaluate(solution, params):
    score = 0
    for route in solution.values():
        time_used = 0
        for task in route:
            score += params["priority_weight"] * task.priority
            time_used += task.service_time
        if time_used > params["time_budget"]:
            score -= params["overtime_penalty"]
    return score


def monte_carlo(workers, tasks, params, local=False):
    best = None
    best_score = -float("inf")

    sims = params["num_simulations_local"] if local else params["num_simulations"]

    for _ in range(sims):
        sol = simulate_solution(workers, tasks, params)
        sc = evaluate(sol, params)

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
        d = math.hypot(w.x - task.x, w.y - task.y)
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

def generate_tasks(n):
    tasks = []
    for i in range(n):
        tasks.append(Task(
            i,
            random.uniform(0, 100),
            random.uniform(0, 100),
            random.randint(0, 6),
            service_time=random.uniform(60, 180),
            created_at=random.uniform(0, 100),
            av=True
        ))
    return tasks


def generate_workers(n):
    workers = []
    for i in range(n):
        workers.append(Worker(i, random.uniform(0, 100), random.uniform(0, 100)))
    return workers


def run_simulation():
    params = default_params()

    workers = generate_workers(4)
    tasks = generate_tasks(1000)

    start = time.time()
    solution = monte_carlo(workers, tasks, params)
    end = time.time()

    print("Initial solution time:", end - start)

    for w in workers:
        w.route = solution[w.id]
        print("Worker ", w.id, ": ", w.route)

    ## simulate new task
    #new_task = Task(999, 50, 50, 6, 30)
    #reschedule(new_task, workers, tasks, params)

    total_tasks = sum(len(w.route) for w in workers)
    print("Total assigned tasks after reschedule:", total_tasks)

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
