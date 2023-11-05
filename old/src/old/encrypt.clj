(ns old.encrypt
  (:import org.mindrot.jbcrypt.BCrypt))

(defn hashpw [pw] (BCrypt/hashpw pw (BCrypt/gensalt 12)))

(defn checkpw [pw hashed-pw] (BCrypt/checkpw pw hashed-pw))

(comment

  (def hashed-pw (hash "abc"))

  (checkpw "abc" hashed-pw)

  hashed-pw

)
