(ns virtual-pmap.core
  (:gen-class)
  (:import (java.util.concurrent Executors Future)))

(defn vpmap
  "Like map, but f is applied in parallel using Java virtual threads.
   Semi-lazy in that the parallel computation stays ahead of consumption.
   Options:
   :t-limit - Maximum number of virtual threads to use (optional)"
  [& args]
  (let [[opts f & colls] (if (map? (first args))
                          args
                          (cons {} args))
        executor (if (:t-limit opts)
                  (Executors/newFixedThreadPool (:t-limit opts))
                  (Executors/newVirtualThreadPerTaskExecutor))
        step (fn step [cs]
               (lazy-seq
                 (let [ss (map seq cs)]
                   (when (every? identity ss)
                     (cons (map first ss) (step (map rest ss)))))))
        futures (try
                 (doall
                   (map-indexed #(.submit executor
                                        ^Callable (fn []
                                                  (.setName (Thread/currentThread) (str "virtual-thread-" %1))
                                                  (apply f %2)))
                               (step colls)))
                 (catch Exception e
                   (.close executor)
                   (throw e)))
        results (try
                 (doall (map #(.get ^Future %) futures))
                 (finally
                   (.close executor)))]
    results))
