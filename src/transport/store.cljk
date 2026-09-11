(ns transport.store
  "SSoT for the ISCO-08 8331 independent passenger transport practice
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a vehicle-inspection and
  boarding-log robot performs pre-trip inspection checklist logging
  and passenger-count tracking under this advisor/governor pair,
  which never dispatches hardware itself and never dispatches a
  route above the vehicle's registered passenger-capacity ceiling).
  Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client  — a registered charter group/school/event organizer
              (:client-id, :name)
    vehicle — a registered vehicle {:vehicle-id :client-id :name
              :max-passenger-capacity number
              :pre-trip-inspection-passed? boolean}.
              `:max-passenger-capacity` is the registered capacity
              ceiling a proposed route dispatch's passenger count must
              not exceed — dispatching beyond the vehicle's registered
              passenger-capacity ceiling is an overcrowded dispatch,
              not efficient scheduling. `:pre-trip-inspection-passed?`
              records whether the vehicle's pre-trip inspection has
              passed — dispatching a route without a passed pre-trip
              inspection is an unsafe departure, not efficient
              service.
    record  — a committed operating record (a dispatched route) —
              written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (vehicle [s vehicle-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-vehicle! [s v])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (vehicle [_ vehicle-id] (get-in @a [:vehicles vehicle-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-vehicle! [s v]
    (swap! a assoc-in [:vehicles (:vehicle-id v)] v) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :vehicles {} :records [] :ledger []}
                                   seed)))))
