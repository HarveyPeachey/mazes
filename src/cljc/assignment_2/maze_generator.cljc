(ns assignment-2.maze-generator "Maze Generator"
  (:require [clojure.string :as str]))

; This Blog was used to help produce Maze algorithms in clojure using both psuedo code and ruby example code
; http://weblog.jamisbuck.org/2011/2/7/maze-generation-algorithm-recap.html

; Maze Grid Creation
; -------------------------------------------------------------------------------------------------------------------
(defn make-a-row [columns]
  (loop [count 0 row []]
      (if (= columns count)
          row
          (recur (inc count) (conj row {:north 0 :east 0 :south 0 :west 0 :visited 0})))))

(defn make-a-grid [rows columns]
  (loop [count 0 grid []]
      (if (= rows count)
          grid
          (recur (inc count) (conj grid (make-a-row columns))))))

(def grid (atom (make-a-grid 10 10)))

(defn reset-grid [size]
  (reset! grid (make-a-grid size size)))

; Maze Helper Functions
; -------------------------------------------------------------------------------------------------------------------
(defn lookup-x-carve [direction]
  (let [dir-x-lookup {"N" 1 "E" 0 "S" -1 "W" 0}]
    (get dir-x-lookup direction)))

(defn lookup-y-carve [direction]
  (let [dir-y-lookup {"N" 0 "E" 1 "S" 0 "W" -1}]
    (get dir-y-lookup direction)))

(defn toprow? [row no-of-rows]
  (= row (- no-of-rows 1)))

(defn lastcolumn? [col no-of-cols]
  (= col (- no-of-cols 1)))

; Maze Generation Algorithms
; -------------------------------------------------------------------------------------------------------------------
(defn binary-tree
  ([row] (binary-tree row 0))
  ([row col]
   (let [no-of-rows (count @grid) no-of-cols (count (first @grid))]
     (cond
       (= row no-of-rows) @grid
       (and (toprow? row no-of-rows) (lastcolumn? col no-of-cols)) 0
       (toprow? row no-of-rows) (do
                                  (swap! grid assoc-in [row col :east] 1)
                                  (swap! grid assoc-in [row (inc col) :west] 1))
       (lastcolumn? col no-of-cols) (do
                                      (swap! grid assoc-in [row col :north] 1)
                                      (swap! grid assoc-in [(inc row) col :south] 1))
       :else
       (if (= 0 (rand-int 2))
         (do
           (swap! grid assoc-in [row col :east] 1)
           (swap! grid assoc-in [row (inc col) :west] 1))
         (do
           (swap! grid assoc-in [row col :north] 1)
           (swap! grid assoc-in [(inc row) col :south] 1)))))))

(defn sidewinder [row]
  (let [no-of-rows (count @grid)
        no-of-cols (count (first @grid))]
    (loop [run-start 0 col 0]
      (letfn [(carvenorth? [] (and (< row (dec no-of-rows)) (or (lastcolumn? col no-of-cols) (= 0 (rand-int 2)))))]
        (cond
          (= col no-of-cols) @grid
          (carvenorth?) (do
                          (let [cell (+ run-start (rand-int (inc (- col run-start))))]
                            (swap! grid assoc-in [row cell :north] 1)
                            (swap! grid assoc-in [(inc row) cell :south] 1)
                            (recur (inc col) (inc col))))
          (< (inc col) no-of-cols) (do
                                     (swap! grid assoc-in [row col :east] 1)
                                     (swap! grid assoc-in [row (inc col) :west] 1)
                                     (recur run-start (inc col))))))))

(defn recursive-backtracker [x y]
  (let [directions (shuffle ["N" "E" "S" "W"])
        no-of-rows (count @grid)
        no-of-cols (count (first @grid))]
    (dotimes [direction (count directions)]
      (do
        (swap! grid assoc-in [x y :visited] 1)
        (let [nx (+ x (lookup-x-carve (get directions direction)))
              ny (+ y (lookup-y-carve (get directions direction)))]
          (if (and (<= 0 nx (- no-of-rows 1)) (<= 0 ny (- no-of-cols 1)) (= (:visited (get-in @grid [nx ny])) 0))
            (cond
              (= (get directions direction) "N") (do
                                                   (swap! grid assoc-in [x y :north] 1)
                                                   (swap! grid assoc-in [nx ny :south] 1)
                                                   (recursive-backtracker nx ny))
              (= (get directions direction) "E") (do
                                                   (swap! grid assoc-in [x y :east] 1)
                                                   (swap! grid assoc-in [nx ny :west] 1)
                                                   (recursive-backtracker nx ny))
              (= (get directions direction) "S") (do
                                                   (swap! grid assoc-in [x y :south] 1)
                                                   (swap! grid assoc-in [nx ny :north] 1)
                                                   (recursive-backtracker nx ny))
              (= (get directions direction) "W") (do
                                                   (swap! grid assoc-in [x y :west] 1)
                                                   (swap! grid assoc-in [nx ny :east] 1)
                                                   (recursive-backtracker nx ny)))))))))

; Maze Generation Entry Function
; -------------------------------------------------------------------------------------------------------------------
(defn carve-passages
  ([] (carve-passages 0 "bt"))
  ([row algorithm]
   (if (= row (count @grid))
     @grid
     (do
       (if (= algorithm "bt")
         (dotimes [col (count (first @grid))]
           (binary-tree row col))
         (sidewinder row))
       (carve-passages (inc row) algorithm))))
  ([x y algorithm]
   (if (= algorithm "rb")
     (if (= x -1)
       @grid
       (do
         (recursive-backtracker x y)
         (carve-passages -1 0 "rb"))))))
