# Virtual pmap

A high-performance parallel mapping library for Clojure leveraging Java's Virtual Threads. This library provides `vpmap`, an alternative to Clojure's built-in `pmap` that offers improved performance through the use of Java's Virtual Thread per task execution model.

## Requirements

- Java 21+ (for Virtual Thread support)

## Installation

Add the following to your `deps.edn`:

```clojure
{:deps {org.clojars.bigsy/virtual-pmap {:mvn/version "0.1.0"}}}
```

## Features

- Parallel execution using Java Virtual Threads
- Thread naming for improved debugging
- Preserves collection order
- 
## Usage
Just like pmap
```clojure
(require '[virtual-pmap.core :refer [vpmap]])

;; Basic usage with a single collection
(vpmap inc [1 2 3 4 5])
;; => [2 3 4 5 6]

;; Multiple collections
(vpmap + [1 2 3] [4 5 6])
;; => [5 7 9]

;; With more complex operations
(vpmap #(do 
          (Thread/sleep 1000) 
          (inc %)) 
       (range 5))
;; => [1 2 3 4 5] ;; Executes in parallel
```

## Performance
- Faster execution compared to Clojure's built-in `pmap`
- More efficient resource utilization through Virtual Threads
- Lower overhead for thread creation and management
- Better scaling with large numbers of parallel operations


## Todo
- Add rate limiting via semaphores
