(ns virtual-pmap.core
  (:gen-class)
  (:import (java.util.concurrent Executors Future)))

(defn vpmap
  "Like map, but f is applied in parallel using Java virtual threads.
   Semi-lazy in that the parallel computation stays ahead of consumption."
  ([f coll]
   (let [n (count coll)
         executor (Executors/newVirtualThreadPerTaskExecutor)
         futures (try
                   (doall
                     (map-indexed #(.submit executor
                                            ^Callable (fn []
                                                        (.setName (Thread/currentThread) (str "virtual-thread-" %1))
                                                        (f %2)))
                                   coll))
                   (catch Exception e
                     (.close executor)
                     (throw e)))
         results (try
                   (doall (map #(.get ^Future %) futures))
                   (finally
                     (.close executor)))]
     results))
  ([f coll & colls]
   (let [step (fn step [cs]
                (lazy-seq
                  (let [ss (map seq cs)]
                    (when (every? identity ss)
                      (cons (map first ss) (step (map rest ss)))))))
         executor (Executors/newVirtualThreadPerTaskExecutor)
         futures (try
                   (doall
                     (map-indexed #(.submit executor
                                            ^Callable (fn []
                                                        (.setName (Thread/currentThread) (str "virtual-thread-" %1))
                                                        (apply f %2)))
                                   (step (cons coll colls))))
                   (catch Exception e
                     (.close executor)
                     (throw e)))
         results (try
                   (doall (map #(.get ^Future %) futures))
                   (finally
                      (.close executor)))]
     results)))

(defn -main [& args]
  (println "Virtual PMap Example:")
  (let [numbers (range 10)
        result (vpmap #(do 
                        (Thread/sleep 100) 
                        (* % %)) 
                     numbers)]
    (println "Squaring numbers in parallel:" result))
  (System/exit 0))
