(ns dvb.server.http.operations.utils.declojurify)

(defn maybe-instant->str [maybe-instant]
  (when maybe-instant (str maybe-instant)))

(defn common [entity]
  (-> entity
      (update :id str)
      (update :created-at maybe-instant->str)
      (update :updated-at maybe-instant->str)
      (update :destroyed-at maybe-instant->str)))

(defn user [user*]
  (-> user*
      common
      (dissoc :password)))

(defn api-key [api-key*]
  (-> api-key*
      (update :id str)
      (update :user-id str)
      (update :created-at maybe-instant->str)
      (update :expires-at maybe-instant->str)))

(defn form [form*]
  (-> form*
      common))
