(ns caponia.query
  (:use [stemmers.core :only [stems]]))

(defn query
  "Retrieve all matches for `query-string`'s stems from `index`."
  [index query-string]
  (let [all-stems (stems query-string (:stemmer @index))]
    (into {}
          (for [stem all-stems]
            [stem (get-in @index [:data stem])]))))

(defn merge-and
  "Process query results by AND; only return `[id weight]` which
  matched all stems."
  [query-results]
  (let [ids (set (mapcat (comp keys second) query-results))
        all-vals (vals query-results)]
    (into {}
      (filter #(> (second %) 0)
              (for [id ids]
                [id (apply *
                           (for [result-set all-vals]
                             (get result-set id 0)))])))))

(defn merge-or
  "Get all matched `[id weight]`, where weight is the sum of
  all occurrences."
  [query-results]
  (apply merge-with + (vals query-results)))

(defn do-search
  "Do a search, merging results as indicated by merge-style.
  Defaults to `:and`."
  ([index term]
   (do-search index term :and))
  ([index term merge-style]
   (let [merge-func (if (= merge-style :and) merge-and merge-or)]
     (reverse (sort-by second (merge-func (query index term)))))))
