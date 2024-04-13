(ns dvb.common.fs
  "File system utilities. See also:
  - https://github.com/clj-commons/fs
  - https://clojure.org/api/cheatsheet the I/O section of the cheatsheet"
  (:require [clojure.java.io :as io]
            [pantomime.mime :as pantomime]
            [clojure.java.shell :as sh])
  #_(:import (java.io OutputStream)
           (java.security MessageDigest DigestInputStream))
  (:refer-clojure :exclude [exists? name identical?]))

(defn symlink [src dest]
  (sh/sh "ln" "-s" src dest))

(defn exists? [p] (.exists (io/file p)))

(defn dir? [p] (.isDirectory (io/file p)))

(defn file? [p] (and (exists? p)
                     (not (dir? p))))

(defn name [p] (.getName (io/file p)))

(defn path [p] (.getPath (io/file p)))

(defn parent [p] (.getParent (io/file p)))

(defn mkdir [p] (.mkdir (io/file p)))

(defn ls [p] (vec (.list (io/file p))))

(defn ls-obj
  "Return a vec of file objects"
  [p] (vec (.listFiles (io/file p))))

(defn cp [src dst] (io/copy (io/file src) (io/file dst)))

(defn rm [p] (io/delete-file p))

(defn mv [src dst]
  (cp src dst)
  (rm src))

#_(defn each-line [path f]
  (with-open [rdr (io/reader path)]
    (mapv f (line-seq rdr))))

(defn append [path s]
  #?(:clj (spit path s :append true)
     :cljs (throw (ex-info "Not implemented." {:path path
                                               :s s}))))

#_(defn write-one-line-at-a-time [path lines]
  (with-open [wrtr (io/writer path)]
    (doseq [i lines]
      (.write wrtr (str i "\n")))))

#_(defn hash [path]
  (let [sink (OutputStream/nullOutputStream)
        digest (MessageDigest/getInstance "MD5")]
    (with-open [input-stream  (io/input-stream path)
                digest-stream (DigestInputStream. input-stream digest)
                output-stream (io/output-stream sink)]
      (io/copy digest-stream output-stream))
    (format "%032x" (BigInteger. 1 (.digest digest)))))

(defn identical? [path1 path2] (= (hash path1) (hash path2)))

(defn size [file-path]
  (when (file? file-path)
    (.length (io/file file-path))))

(defn mime-type [file-path]
  (pantomime/mime-type-of (io/file file-path)))

(defn mime-type-of-file [file-object]
  (pantomime/mime-type-of file-object))
