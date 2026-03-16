# Local Search VRP

A hybrid Java solver for the Capacitated Vehicle Routing Problem (CVRP), built around exact optimization for small instances and adaptive local search for larger ones. For small instances, the solver uses IBM CPLEX to recover optimal routes. For larger instances, it switches to a greedy best-improvement search over relocate and swap neighborhoods, then perturbs the incumbent solution to escape local minima. This project was developed for Brown's `CSCI 2951-O` and performed strongly in the course competition on both runtime and solution quality. Find the idea in further detail on my portfolio [project page](https://kyranflynn.me/projects/local-search-vrp/).

## What It Does

- Solves small instances exactly with an integer programming model.
- Solves larger instances with constructive heuristics plus disturbed local search.
- Builds initial routes with savings, sweep, and CP-based feasible construction.
- Improves routes through relocate and swap neighborhoods across all active routes.
- Tracks repeated returns to similar local minima using objective-value proxies and increases disturbance strength when the search keeps revisiting the same region.

## Approach

The large-instance solver is centered on greedy local search: at each step it evaluates neighboring solutions produced by customer relocation and customer swaps, then applies an improving move. That baseline is intentionally simple, but the main idea is how the solver reacts once the search stalls.

Instead of restarting blindly, it records lightweight proxies for previously reached minima and treats repeated returns to similar objective values as evidence that the search is trapped on the same local surface. When that happens, it increases the number of random disturbances applied before resuming best-improvement search. In practice, that preserves the speed of greedy local search while making it much better at escaping shallow local basins.

## Repo Layout

- `src/solver/complete` - exact models
- `src/solver/incomplete` - local search, perturbation, and checking logic
- `src/solver/initial` - initial solution builders
- `input` - sample `.vrp` benchmark instances
- `solutions` - saved solver outputs

## Quick Start

### Requirements

- Java
- IBM ILOG CPLEX / CP Optimizer

The provided scripts are currently wired to the Brown department machine install paths for CPLEX. If you are running elsewhere, update the classpath and library paths in `compile.sh` and `run.sh`.

### Compile

```bash
./compile.sh
```

### Run One Instance

```bash
./run.sh input/101_8_1.vrp
```

### Run a Batch

```bash
./runAll.sh input/ 285 results.log
```

## Input and Output

Each input starts with:

```text
<numCustomers> <numVehicles> <vehicleCapacity>
```

followed by one customer per line:

```text
<demand> <x> <y>
```

The solver prints a JSON-style summary to stdout and writes solution files to `solutions/<instance>.sol`.

## Why This Project

This repo is a compact example of combining exact optimization, classical VRP construction heuristics, and a pragmatic local-search escape mechanism that uses remembered local structure instead of pure random restarts.
